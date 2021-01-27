package grakn.core.reasoner.resolution.framework;

import grakn.core.common.exception.GraknException;
import grakn.core.concurrent.actor.Actor;
import grakn.core.reasoner.resolution.ResolverRegistry;
import grakn.core.traversal.TraversalEngine;

import static grakn.core.common.exception.ErrorMessage.Internal.ILLEGAL_STATE;

public class InterceptingResolver<T extends Resolver<T>> extends Resolver<T> {

    private final Resolver<?> resolver;

    protected InterceptingResolver(Actor<T> self, String name, ResolverRegistry registry, TraversalEngine traversalEngine, boolean explanations, Resolver<?> resolver) {
        super(self, name, registry, traversalEngine, explanations);
        this.resolver = resolver;
    }

    @Override
    public void receiveRequest(Request fromUpstream, int iteration) {
        resolver.receiveRequest(fromUpstream, iteration);
    }

    @Override
    protected void receiveAnswer(Response.Answer fromDownstream, int iteration) {
        resolver.receiveAnswer(fromDownstream, iteration);
    }

    @Override
    protected void receiveExhausted(Response.Exhausted fromDownstream, int iteration) {
        resolver.receiveExhausted(fromDownstream, iteration);
    }

    @Override
    protected void requestFromDownstream(Request request, Request fromUpstream, int iteration) {
        super.requestFromDownstream(request, fromUpstream, iteration);
    }

    @Override
    protected void respondToUpstream(Response response, int iteration) {
        super.respondToUpstream(response, iteration);
    }






    @Override
    protected void initialiseDownstreamActors() {
        throw GraknException.of(ILLEGAL_STATE);
    }

    @Override
    protected ResponseProducer responseProducerCreate(Request fromUpstream, int iteration) {
        throw GraknException.of(ILLEGAL_STATE);
    }

    @Override
    protected ResponseProducer responseProducerReiterate(Request fromUpstream, ResponseProducer responseProducer, int newIteration) {
        throw GraknException.of(ILLEGAL_STATE);
    }

    @Override
    protected void exception(Exception e) {

    }


}
