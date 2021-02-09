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

package grakn.core.reasoner.benchmark;


import grakn.core.Grakn;
import grakn.core.common.parameters.Arguments;
import grakn.core.common.parameters.Options;
import grakn.core.concept.answer.ConceptMap;
import grakn.core.rocks.RocksGrakn;
import grakn.core.test.integration.util.Util;
import graql.lang.Graql;
import graql.lang.pattern.Pattern;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RuleScalingTest {

    private static final Path directory = Paths.get(System.getProperty("user.dir")).resolve("rule-scaling-test");
    private static final String database = "rule-scaling-test";
    private static RocksGrakn grakn;

    @Before
    public void setUp() throws IOException {
        Util.resetDirectory(directory);
        grakn = RocksGrakn.open(directory);
        grakn.databases().create(database);
    }

    @After
    public void tearDown() {
        grakn.close();
    }

    @Test
    public void ruleScaling() {
        final int N = 40;
        final int populatedChains = 3;
        Grakn.Session session = grakn.session(database, Arguments.Session.Type.SCHEMA);
        setup(session, N);
        session.close();
        session = grakn.session(database, Arguments.Session.Type.DATA);

        // note: roles do not have to be shared, specific relation always used to query
        // can generate new roles with relation

        try (Grakn.Transaction tx = session.transaction(Arguments.Transaction.Type.WRITE)) {
            for (int k = 0; k < populatedChains; k++) {
                tx.query().insert(Graql.parseQuery(
                        "insert " +
                                "$x isa someEntity;" +
                                "$y isa intermediateEntity;" +
                                "$link isa intermediateEntity;" +
                                "$index isa intermediateEntity;" +
                                "$anotherLink isa finalEntity;" +
                                "(br-role: $x, br-anotherRole: $y) isa baseRelation;" +
                                "(abr-role: $y, abr-anotherRole: $link) isa anotherBaseRelation;" +
                                "(sr-" + k + "-role: $link, sr-" + k + "-anotherRole: $index) isa specificRelation" + k + ";" +
                                "(asr-" + k + "-role: $index, asr-" + k + "-anotherRole: $anotherLink) isa anotherSpecificRelation" + k + ";"
                ));
            }
            tx.commit();
        }


        try (Grakn.Transaction tx = session.transaction(Arguments.Transaction.Type.READ, new Options.Transaction().infer(true))) {
            long start = System.currentTimeMillis();
            tx.logic().rules().toList();
            System.out.println("Time to load all rules: " + (System.currentTimeMillis() - start));
            String query = "match " +
                    "$x isa someEntity;" +
                    "(br-role: $x, br-anotherRole: $y) isa baseRelation;" +
                    "(abr-role: $y, abr-anotherRole: $link) isa anotherBaseRelation;" +
                    "$r (dr-role: $link, dr-anotherRole: $anotherLink) isa derivedRelation;" +
                    "$r has inferredAttribute $value;" +
                    "$r has anotherInferredAttribute $anotherValue;" +
                    "(ir-role: $anotherLink, ir-anotherRole: $index) isa indexingRelation;";
            List<ConceptMap> answers = executeQuery(query, tx);
            assertEquals(populatedChains, answers.size());
        }
        session.close();
    }

    private void setup(Grakn.Session session, int N) {
        setupSchema(session, N);
        setupRules(session, N);
    }

    private void setupSchema(Grakn.Session session, int N) {
        try (Grakn.Transaction tx = session.transaction(Arguments.Transaction.Type.WRITE)) {
            tx.query().define(Graql.parseQuery(
                    "define " +
                            "baseEntity sub entity," +
                            "plays baseRelation:br-role, plays baseRelation:br-anotherRole," +
                            "plays anotherBaseRelation:abr-role, plays anotherBaseRelation:abr-anotherRole," +
                            "plays derivedRelation:dr-role, plays derivedRelation:dr-anotherRole," +
                            "plays indexingRelation:ir-role, plays indexingRelation:ir-anotherRole;" +
                            "someEntity sub baseEntity;" +
                            "intermediateEntity sub baseEntity;" +
                            "finalEntity sub baseEntity;" +
                            "baseRelation sub relation, relates br-role, relates br-anotherRole;" +
                            "anotherBaseRelation sub relation, relates abr-role, relates abr-anotherRole;" +

                            "derivedRelation sub relation, " +
                            "   owns inferredAttribute, owns anotherInferredAttribute," +
                            "   relates dr-role, relates dr-anotherRole;" +
                            "indexingRelation sub relation, relates ir-role, relates ir-anotherRole;" +
                            "inferredAttribute sub attribute, value string;" +
                            "anotherInferredAttribute sub attribute, value string;"
            ));

            for (int i = 0; i < N; i++) {
                tx.query().define(Graql.parseQuery(
                        "define " +
                                "specificRelation" + i + " sub relation," +
                                "  relates sr-" + i + "-role," +
                                "  relates sr-" + i + "-anotherRole;" +
                                "anotherSpecificRelation" + i + " sub relation," +
                                "  relates asr-" + i + "-role," +
                                "  relates asr-" + i + "-anotherRole;" +
                                "baseEntity sub entity," +
                                "  plays specificRelation" + i + ":" + "sr-" + i + "-role," +
                                "  plays specificRelation" + i + ":" + "sr-" + i + "-anotherRole," +
                                "  plays anotherSpecificRelation" + i + ":" + "asr-" + i + "-role," +
                                "  plays anotherSpecificRelation" + i + ":" + "asr-" + i + "-anotherRole;"

                ).asDefine());
            }
            tx.commit();
        }
    }

    private void setupRules(Grakn.Session session, int N) {
        String basePattern =
                "$x isa someEntity;" +
                        "(br-role: $x, br-anotherRole: $y) isa baseRelation;" +
                        "(abr-role: $y, abr-anotherRole: $link) isa anotherBaseRelation;";

        try (Grakn.Transaction tx = session.transaction(Arguments.Transaction.Type.WRITE)) {
            for (int i = 0; i < N; i++) {
                Pattern specificPattern = Graql.parsePattern(
                        "{" +
                                basePattern +
                                "(sr-" + i + "-role: $link, sr-" + i + "-anotherRole: $index) isa specificRelation" + i + ";" +
                                "(asr-" + i + "-role: $index, asr-" + i + "-anotherRole: $anotherLink) isa anotherSpecificRelation" + i + ";" +
                                "}"
                );

                tx.logic().putRule("relationRule" + i,
                                   Graql.and(specificPattern),
                                   Graql.parseVariable("(dr-role: $link, dr-anotherRole: $anotherLink) isa derivedRelation").asThing());


                tx.logic().putRule("attributeRule" + i,
                                   Graql.and(specificPattern, Graql.parsePattern("{ $r (dr-role: $link, dr-anotherRole: $anotherLink) isa derivedRelation; }")),
                                   Graql.parseVariable("$r has inferredAttribute 'inferredValue" + i + "'").asThing()
                );

                tx.logic().putRule("anotherAttributeRule" + i,
                                   Graql.and(specificPattern,
                                             Graql.parsePattern("{ $r (dr-role: $link, dr-anotherRole: $anotherLink) isa derivedRelation; }"),
                                             Graql.parsePattern("{ $r has inferredAttribute 'inferredValue" + i + "'; }")),
                                   Graql.parseVariable("$r has anotherInferredAttribute 'anotherInferredValue" + i + "'").asThing()
                );


                tx.logic().putRule("indexingRelationRule" + i,
                                   Graql.and(specificPattern,
                                             Graql.parsePattern("{ $r (dr-role: $link, dr-anotherRole: $anotherLink) isa derivedRelation; }"),
                                             Graql.parsePattern("{ $r has inferredAttribute 'inferredValue" + i + "'; }"),
                                             Graql.parsePattern("{ $r has anotherInferredAttribute 'anotherInferredValue" + i + "'; }")),
                                   Graql.parseVariable("(ir-role: $anotherLink, ir-anotherRole: $index) isa indexingRelation").asThing()
                );
            }
            tx.commit();
        }
    }

    private List<ConceptMap> executeQuery(String queryString, Grakn.Transaction transaction) {
        final long startTime = System.currentTimeMillis();
        List<ConceptMap> results = transaction.query().match(Graql.parseQuery(queryString).asMatch()).toList();
        final long answerTime = System.currentTimeMillis() - startTime;
        System.out.println("Query results = " + results.size() + " answerTime: " + answerTime);
        return results;
    }

}


