/*
 * Copyright (C) 2021 Grakn Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package grakn.core.reasoner.resolution.resolver;

import grakn.core.common.iterator.Iterators;
import grakn.core.common.iterator.ResourceIterator;
import grakn.core.concept.ConceptManager;
import grakn.core.concept.answer.ConceptMap;
import grakn.core.concurrent.actor.Actor;
import grakn.core.logic.LogicManager;
import grakn.core.logic.resolvable.Concludable;
import grakn.core.logic.resolvable.Resolvable;
import grakn.core.logic.resolvable.Retrievable;
import grakn.core.reasoner.resolution.Planner;
import grakn.core.reasoner.resolution.ResolutionRecorder;
import grakn.core.reasoner.resolution.ResolverRegistry;
import grakn.core.reasoner.resolution.answer.AnswerState;
import grakn.core.reasoner.resolution.answer.Mapping;
import grakn.core.reasoner.resolution.framework.Request;
import grakn.core.reasoner.resolution.framework.ResolutionAnswer;
import grakn.core.reasoner.resolution.framework.Resolver;
import grakn.core.reasoner.resolution.framework.Response;
import grakn.core.reasoner.resolution.framework.ResponseProducer;
import grakn.core.traversal.TraversalEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static grakn.common.collection.Collections.map;
import static grakn.core.common.iterator.Iterators.iterate;

public interface Root {

    void submitAnswer(ResolutionAnswer answer);

    void submitFail(int iteration);

    class Conjunction extends Resolver<Conjunction> implements Root {

        private static final Logger LOG = LoggerFactory.getLogger(Conjunction.class);

        private final ConceptManager conceptMgr;
        private final LogicManager logicMgr;
        private final grakn.core.pattern.Conjunction conjunction;
        private final Planner planner;
        private final Actor<ResolutionRecorder> resolutionRecorder;
        private final Consumer<ResolutionAnswer> onAnswer;
        private final Consumer<Integer> onFail;
        private final List<Resolvable> plan;
        private boolean isInitialised;
        private ResponseProducer responseProducer;
        private final Map<Resolvable, ResolverRegistry.AlphaEquivalentResolver> downstreamResolvers;

        public Conjunction(Actor<Conjunction> self, grakn.core.pattern.Conjunction conjunction, Consumer<ResolutionAnswer> onAnswer,
                           Consumer<Integer> onFail, Actor<ResolutionRecorder> resolutionRecorder, ResolverRegistry registry,
                           TraversalEngine traversalEngine, ConceptManager conceptMgr, LogicManager logicMgr, Planner planner, boolean explanations) {
            super(self, Conjunction.class.getSimpleName() + "(pattern:" + conjunction + ")", registry, traversalEngine, explanations);
            this.conjunction = conjunction;
            this.onAnswer = onAnswer;
            this.onFail = onFail;
            this.resolutionRecorder = resolutionRecorder;
            this.conceptMgr = conceptMgr;
            this.logicMgr = logicMgr;
            this.planner = planner;
            this.isInitialised = false;
            this.plan = new ArrayList<>();
            this.downstreamResolvers = new HashMap<>();
        }

        @Override
        public void submitAnswer(ResolutionAnswer answer) {
            LOG.debug("Submitting answer: {}", answer.derived());
            if (explanations()) {
                LOG.trace("Recording root answer: {}", answer);
                resolutionRecorder.tell(state -> state.record(answer));
            }
            onAnswer.accept(answer);
        }

        @Override
        public void submitFail(int iteration) {
            onFail.accept(iteration);
        }

        @Override
        public void receiveRequest(Request fromUpstream, int iteration) {
            LOG.trace("{}: received Request: {}", name(), fromUpstream);
            if (!isInitialised) {
                initialiseDownstreamActors();
                isInitialised = true;
                responseProducer = responseProducerCreate(fromUpstream, iteration);
            }
            mayReiterateResponseProducer(fromUpstream, iteration);
            if (iteration < responseProducer.iteration()) {
                // short circuit if the request came from a prior iteration
                onFail.accept(iteration);
            } else {
                assert iteration == responseProducer.iteration();
                tryAnswer(fromUpstream, iteration);
            }
        }

        @Override
        protected void receiveAnswer(Response.Answer fromDownstream, int iteration) {
            LOG.trace("{}: received answer: {}", name(), fromDownstream);

            Request toDownstream = fromDownstream.sourceRequest();
            Request fromUpstream = fromUpstream(toDownstream);

            ResolutionAnswer.Derivation derivation;
            if (explanations()) {
                derivation = fromDownstream.sourceRequest().partialResolutions();
                if (fromDownstream.answer().isInferred()) {
                    derivation = derivation.withAnswer(fromDownstream.sourceRequest().receiver(), fromDownstream.answer());
                }
            } else {
                derivation = null;
            }

            ConceptMap conceptMap = fromDownstream.answer().derived().withInitialFiltered();
            if (fromDownstream.planIndex() == plan.size() - 1) {
                assert fromUpstream.filter().isPresent();
                AnswerState.UpstreamVars.Derived answer = fromUpstream.partialAnswer().asIdentity()
                        .aggregateToUpstream(conceptMap, fromUpstream.filter().get());
                ConceptMap filteredMap = answer.withInitialFiltered();
                if (!responseProducer.hasProduced(filteredMap)) {
                    responseProducer.recordProduced(filteredMap);
                    ResolutionAnswer resolutionAnswer = new ResolutionAnswer(answer, conjunction.toString(), derivation, self(),
                                                                             fromDownstream.answer().isInferred());
                    submitAnswer(resolutionAnswer);
                } else {
                    tryAnswer(fromUpstream, iteration);
                }
            } else {
                int planIndex = fromDownstream.planIndex() + 1;
                ResolverRegistry.AlphaEquivalentResolver nextPlannedDownstream = downstreamResolvers.get(plan.get(planIndex));
                Request downstreamRequest = Request.create(fromUpstream.path().append(nextPlannedDownstream.resolver()),
                                                           AnswerState.UpstreamVars.Initial.of(conceptMap).toDownstreamVars(
                                                                   Mapping.of(nextPlannedDownstream.mapping())),
                                                           derivation, planIndex, null);
                responseProducer.addDownstreamProducer(downstreamRequest);
                requestFromDownstream(downstreamRequest, fromUpstream, iteration);
            }
        }

        @Override
        protected void receiveExhausted(Response.Fail fromDownstream, int iteration) {
            LOG.trace("{}: received Exhausted, with iter {}: {}", name(), iteration, fromDownstream);
            Request toDownstream = fromDownstream.sourceRequest();
            Request fromUpstream = fromUpstream(toDownstream);

            if (iteration < responseProducer.iteration()) {
                // short circuit old iteration exhausted messages back out of the actor model
                submitFail(iteration);
                return;
            }

            responseProducer.removeDownstreamProducer(fromDownstream.sourceRequest());
            tryAnswer(fromUpstream, iteration);
        }

        @Override
        protected void initialiseDownstreamActors() {
            LOG.debug("{}: initialising downstream actors", name());
            Set<Concludable> concludables = Iterators.iterate(Concludable.create(conjunction))
                    .filter(c -> c.getApplicableRules(conceptMgr, logicMgr).hasNext()).toSet();
            if (concludables.size() > 0) {
                Set<Retrievable> retrievables = Retrievable.extractFrom(conjunction, concludables);
                Set<Resolvable> resolvables = new HashSet<>();
                resolvables.addAll(concludables);
                resolvables.addAll(retrievables);
                plan.addAll(planner.plan(resolvables));
                iterate(plan).forEachRemaining(resolvable -> {
                    downstreamResolvers.put(resolvable, registry.registerResolvable(resolvable));
                });
            }
        }

        @Override
        protected ResponseProducer responseProducerCreate(Request fromUpstream, int iteration) {
            LOG.debug("{}: Creating a new ResponseProducer for request: {}", name(), fromUpstream);

            assert fromUpstream.filter().isPresent() && fromUpstream.partialAnswer().isIdentity();
            ResourceIterator<AnswerState.UpstreamVars.Derived> upstreamAnswers = traversalEngine.iterator(conjunction.traversal())
                    .map(conceptMgr::conceptMap)
                    .map(conceptMap -> fromUpstream.partialAnswer().asIdentity().aggregateToUpstream(conceptMap, fromUpstream.filter().get()));
            ResponseProducer responseProducer = new ResponseProducer(upstreamAnswers, iteration);
            if (!plan.isEmpty()) {
                Request toDownstream = Request.create(fromUpstream.path().append(downstreamResolvers.get(plan.get(0)).resolver()),
                                                      AnswerState.UpstreamVars.Initial.of(fromUpstream.partialAnswer().conceptMap())
                                                              .toDownstreamVars(Mapping.of(downstreamResolvers.get(plan.get(0)).mapping())),
                                                      new ResolutionAnswer.Derivation(map()), 0, null);
                responseProducer.addDownstreamProducer(toDownstream);
            }
            return responseProducer;
        }

        @Override
        protected ResponseProducer responseProducerReiterate(Request fromUpstream, ResponseProducer responseProducerPrevious,
                                                             int newIteration) {
            assert newIteration > responseProducerPrevious.iteration();
            LOG.debug("{}: Updating ResponseProducer for iteration '{}'", name(), newIteration);

            assert fromUpstream.filter().isPresent() && fromUpstream.partialAnswer().isIdentity();
            ResourceIterator<AnswerState.UpstreamVars.Derived> upstreamAnswers = traversalEngine.iterator(conjunction.traversal())
                    .map(conceptMgr::conceptMap)
                    .map(conceptMap -> fromUpstream.partialAnswer().asIdentity().aggregateToUpstream(conceptMap, fromUpstream.filter().get()));
            ResponseProducer responseProducerNewIter = responseProducerPrevious.newIteration(upstreamAnswers, newIteration);

            if (!plan.isEmpty()) {
                Request toDownstream = Request.create(fromUpstream.path().append(downstreamResolvers.get(plan.get(0)).resolver()),
                                                      AnswerState.UpstreamVars.Initial.of(fromUpstream.partialAnswer().conceptMap()).
                                                              toDownstreamVars(Mapping.of(downstreamResolvers.get(plan.get(0)).mapping())),
                                                      new ResolutionAnswer.Derivation(map()), 0, null);
                responseProducerNewIter.addDownstreamProducer(toDownstream);
            }
            return responseProducerNewIter;
        }

        @Override
        protected void exception(Throwable e) {
            LOG.error("Actor exception", e);
            // TODO, once integrated into the larger flow of executing queries, kill the actors and report and exception to root
        }

        private void tryAnswer(Request fromUpstream, int iteration) {
            if (responseProducer.hasUpstreamAnswer()) {
                AnswerState.UpstreamVars.Derived upstreamAnswer = responseProducer.upstreamAnswers().next();
                responseProducer.recordProduced(upstreamAnswer.withInitialFiltered());
                ResolutionAnswer answer = new ResolutionAnswer(upstreamAnswer, conjunction.toString(),
                                                               ResolutionAnswer.Derivation.EMPTY, self(), false);
                submitAnswer(answer);
            } else {
                if (responseProducer.hasDownstreamProducer()) {
                    requestFromDownstream(responseProducer.nextDownstreamProducer(), fromUpstream, iteration);
                } else {
                    submitFail(iteration);
                }
            }
        }

        private void mayReiterateResponseProducer(Request fromUpstream, int iteration) {
            if (responseProducer.iteration() + 1 == iteration) {
                responseProducer = responseProducerReiterate(fromUpstream, responseProducer, iteration);
            }
        }
    }

    class Disjunction extends Resolver<Disjunction> implements Root {

        private static final Logger LOG = LoggerFactory.getLogger(Disjunction.class);

        private final grakn.core.pattern.Disjunction disjunction;
        private final Actor<ResolutionRecorder> resolutionRecorder;
        private final Consumer<ResolutionAnswer> onAnswer;
        private final Consumer<Integer> onFail;
        private boolean isInitialised;
        private ResponseProducer responseProducer;
        private final List<Actor<ConjunctionResolver>> downstreamResolvers;

        public Disjunction(Actor<Disjunction> self, grakn.core.pattern.Disjunction disjunction, Consumer<ResolutionAnswer> onAnswer,
                           Consumer<Integer> onFail, Actor<ResolutionRecorder> resolutionRecorder, ResolverRegistry registry,
                           TraversalEngine traversalEngine, boolean explanations) {
            super(self, Disjunction.class.getSimpleName() + "(pattern:" + disjunction + ")", registry, traversalEngine, explanations);
            this.disjunction = disjunction;
            this.onAnswer = onAnswer;
            this.onFail = onFail;
            this.resolutionRecorder = resolutionRecorder;
            this.isInitialised = false;
            this.downstreamResolvers = new ArrayList<>();
        }

        @Override
        public void submitAnswer(ResolutionAnswer answer) {
            LOG.debug("Submitting answer: {}", answer.derived());
            if (explanations()) {
                LOG.trace("Recording root answer: {}", answer);
                resolutionRecorder.tell(state -> state.record(answer));
            }
            onAnswer.accept(answer);
        }

        @Override
        public void submitFail(int iteration) {
            onFail.accept(iteration);
        }

        @Override
        public void receiveRequest(Request fromUpstream, int iteration) {
            LOG.trace("{}: received Request: {}", name(), fromUpstream);
            if (!isInitialised) {
                initialiseDownstreamActors();
                isInitialised = true;
                responseProducer = responseProducerCreate(fromUpstream, iteration);
            }
            mayReiterateResponseProducer(fromUpstream, iteration);
            if (iteration < responseProducer.iteration()) {
                // short circuit if the request came from a prior iteration
                onFail.accept(iteration);
            } else {
                assert iteration == responseProducer.iteration();
                tryAnswer(fromUpstream, iteration);
            }
        }

        private void tryAnswer(Request fromUpstream, int iteration) {
            if (responseProducer.hasDownstreamProducer()) {
                requestFromDownstream(responseProducer.nextDownstreamProducer(), fromUpstream, iteration);
            } else {
                submitFail(iteration);
            }
        }

        private void mayReiterateResponseProducer(Request fromUpstream, int iteration) {
            if (responseProducer.iteration() + 1 == iteration) {
                responseProducer = responseProducerReiterate(fromUpstream, responseProducer, iteration);
            }
        }

        @Override
        protected void receiveAnswer(Response.Answer fromDownstream, int iteration) {
            LOG.trace("{}: received answer: {}", name(), fromDownstream);

            Request toDownstream = fromDownstream.sourceRequest();
            Request fromUpstream = fromUpstream(toDownstream);

            ResolutionAnswer.Derivation derivation;
            if (explanations()) {
                // TODO
                derivation = null;
            } else {
                derivation = null;
            }

            ConceptMap conceptMap = fromDownstream.answer().derived().withInitialFiltered();
            assert fromUpstream.filter().isPresent();
            AnswerState.UpstreamVars.Derived answer = fromUpstream.partialAnswer().asIdentity()
                    .aggregateToUpstream(conceptMap, fromUpstream.filter().get());
            ConceptMap filteredMap = answer.withInitialFiltered();
            if (!responseProducer.hasProduced(filteredMap)) {
                responseProducer.recordProduced(filteredMap);
                ResolutionAnswer resolutionAnswer = new ResolutionAnswer(answer, disjunction.toString(), derivation, self(),
                                                                         fromDownstream.answer().isInferred());
                submitAnswer(resolutionAnswer);
            } else {
                tryAnswer(fromUpstream, iteration);
            }
        }

        @Override
        protected void receiveExhausted(Response.Fail fromDownstream, int iteration) {
            LOG.trace("{}: received Exhausted, with iter {}: {}", name(), iteration, fromDownstream);
            Request toDownstream = fromDownstream.sourceRequest();
            Request fromUpstream = fromUpstream(toDownstream);

            if (iteration < responseProducer.iteration()) {
                // short circuit old iteration exhausted messages back out of the actor model
                submitFail(iteration);
                return;
            }
            responseProducer.removeDownstreamProducer(fromDownstream.sourceRequest());
            tryAnswer(fromUpstream, iteration);
        }

        @Override
        protected void initialiseDownstreamActors() {
            LOG.debug("{}: initialising downstream actors", name());
            for (grakn.core.pattern.Conjunction conjunction : disjunction.conjunctions()) {
                downstreamResolvers.add(registry.conjunction(conjunction));
            }
        }

        @Override
        protected ResponseProducer responseProducerCreate(Request fromUpstream, int iteration) {
            LOG.debug("{}: Creating a new ResponseProducer for request: {}", name(), fromUpstream);
            assert fromUpstream.filter().isPresent() && fromUpstream.partialAnswer().isIdentity();
            ResponseProducer responseProducer = new ResponseProducer(Iterators.empty(), iteration);
            assert !downstreamResolvers.isEmpty();
            for (Actor<ConjunctionResolver> conjunctionResolver : downstreamResolvers) {
                Request request = Request.create(fromUpstream.path().append(conjunctionResolver),
                                                 AnswerState.UpstreamVars.Initial.of(fromUpstream.partialAnswer().conceptMap()).toDownstreamVars(),
                                                 ResolutionAnswer.Derivation.EMPTY, -1, null);
                responseProducer.addDownstreamProducer(request);
            }
            return responseProducer;
        }

        @Override
        protected ResponseProducer responseProducerReiterate(Request fromUpstream, ResponseProducer responseProducerPrevious, int newIteration) {
            assert newIteration > responseProducerPrevious.iteration();
            LOG.debug("{}: Updating ResponseProducer for iteration '{}'", name(), newIteration);

            assert newIteration > responseProducerPrevious.iteration();
            ResponseProducer responseProducerNewIter = responseProducerPrevious.newIteration(Iterators.empty(), newIteration);
            for (Actor<ConjunctionResolver> conjunctionResolver : downstreamResolvers) {
                Request request = Request.create(fromUpstream.path().append(conjunctionResolver),
                                                 AnswerState.UpstreamVars.Initial.of(fromUpstream.partialAnswer().conceptMap()).toDownstreamVars(),
                                                 ResolutionAnswer.Derivation.EMPTY, -1, null);
                responseProducer.addDownstreamProducer(request);
            }
            return responseProducerNewIter;
        }

        @Override
        protected void exception(Throwable e) {
            LOG.error("Actor exception", e);
            // TODO, once integrated into the larger flow of executing queries, kill the actors and report and exception to root
        }

    }
}