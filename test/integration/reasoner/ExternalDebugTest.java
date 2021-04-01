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

package grakn.core.reasoner;

import grakn.common.concurrent.NamedThreadFactory;
import grakn.core.common.iterator.FunctionalIterator;
import grakn.core.common.parameters.Arguments;
import grakn.core.common.parameters.Options;
import grakn.core.concept.answer.ConceptMap;
import grakn.core.concurrent.actor.ActorExecutorGroup;
import grakn.core.rocks.RocksGrakn;
import grakn.core.rocks.RocksSession;
import grakn.core.rocks.RocksTransaction;
import grakn.core.test.integration.util.Util;
import graql.lang.Graql;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class ExternalDebugTest {


    /*


    To be able to point at external data directories, you must change the rocksdb dependency to a non-dev file:

    in //rocks:BUILD
    mac_deps = [
        "@maven//:com_google_ortools_ortools_darwin",
        "@maven//:com_google_ortools_ortools_darwin_java",
#        "@maven//:org_rocksdb_rocksdbjni_dev",
        "@maven//:org_rocksdb_rocksdbjni"   ,  # Add this
    ],


    You'll also want the following test options:
    --spawn_strategy=local
    --test_output=streamed
    --cache_test_results=no


     */

    private static final Path dataDir = Paths.get("/Users/joshua/Documents/grakn-debug/grabl/grakn-cluster_grabl_slow_traces_ ee1d4fdff7da4d9471121a9eeda5db2557ad189a/server/data");
    private static final Path logDir = dataDir.getParent().resolve("logs");
    private static final Options.Database options = new Options.Database().dataDir(dataDir).logsDir(logDir);
    private static final String database = "grabl";
    private static RocksGrakn grakn;

    private RocksTransaction singleThreadElgTransaction(RocksSession session, Arguments.Transaction.Type transactionType) {
        RocksTransaction transaction = session.transaction(transactionType, new Options.Transaction().infer(true).parallel(false));
//        ActorExecutorGroup service = new ActorExecutorGroup(1, new NamedThreadFactory("grakn-core-actor"));
//        transaction.reasoner().resolverRegistry().setExecutorService(service);
        return transaction;
    }

    @Before
    public void setUp() throws IOException {
        grakn = RocksGrakn.open(options);
    }

    @After
    public void tearDown() {
        grakn.close();
    }


    /**
     * This query doesn't terminate, but also has a lot of answers. It retrieves all the traces for a particular iteration to plot
     * The performance in pre-explanations appears to be around 2-6 seconds for 10 answers, post explanations seems to be around
     * 16-45 (much higher variability too) for 10 answers
     *
     *
     * Cause of the pain:
     * each time we materialise a relation, we do a GET followed by an insert if required.
     * This GET does a lookup for `$_0 (role1: $x iid 0x123, role2: $y iid 0x234)`
     * This however is very expensive when $x has many `role1` connections - worst case we have to scan each role to get to a $_0
     * from which point we have to check the connection to $y.
     */
    @Test
    public void traces_query() {
        String query = "match\n" +
                "$analysis isa performance-analysis, has analysis-id 7388240783629118464;\n" +
                "$trace isa trace, has iteration 70, has path $path, has duration $duration;\n" +
                "$_ (analysis: $analysis, trace: $trace) isa performance-trace; limit 100;";
        try (RocksSession session = grakn.session(database, Arguments.Session.Type.DATA)) {
            try (RocksTransaction txn = singleThreadElgTransaction(session, Arguments.Transaction.Type.READ)) {
                Instant now = Instant.now();
                List<ConceptMap> answers = txn.query().match(Graql.parseQuery(query).asMatch()).toList();
                Instant end = Instant.now();
                Duration delta = Duration.between(now, end);
                System.out.println("Time elapsed (ms): " + delta.toMillis() + ", retrieved: " + answers.size() + " answers");
            }
        }
    }

    @Test
    public void permissions_query() throws InterruptedException {
        Thread.sleep(5000);
        String query = "match\n" +
                "$owner isa organisation, has name \"graknlabs\" ;\n" +
                "(owner: $owner, repo: $repo) isa repo-owner ;\n" +
                "$repo isa repository, has name $name ;\n" +
                "$user isa user, has name \"lriuui0x0\" ;\n" +
                "(collaborator: $user, repo: $repo) isa repo-collaborator, has permission $permission ;\n" +
                "get $name, $permission;";
        try (RocksSession session = grakn.session(database, Arguments.Session.Type.DATA)) {
            try (RocksTransaction txn = singleThreadElgTransaction(session, Arguments.Transaction.Type.READ)) {
                Instant now = Instant.now();
                List<ConceptMap> answers = txn.query().match(Graql.parseQuery(query).asMatch()).toList();
                Instant end = Instant.now();
                Duration delta = Duration.between(now, end);
                System.out.println("Time elapsed (ms): " + delta.toMillis() + ", retrieved: " + answers.size() + " answers");
            }
        }
    }
}
