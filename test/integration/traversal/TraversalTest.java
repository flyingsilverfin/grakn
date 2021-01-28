package grakn.core.test.integration.traversal;

import grakn.core.*;
import grakn.core.common.iterator.*;
import grakn.core.common.parameters.*;
import grakn.core.concept.answer.*;
import grakn.core.rocks.*;
import grakn.core.test.integration.util.*;
import grakn.core.traversal.*;
import grakn.core.traversal.common.*;
import grakn.core.traversal.procedure.*;
import graql.lang.*;
import graql.lang.query.*;
import org.junit.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import static grakn.common.collection.Collections.*;
import static grakn.core.common.parameters.Arguments.Transaction.Type.*;
import static org.junit.Assert.*;

public class TraversalTest {

    private static Path directory = Paths.get(System.getProperty("user.dir")).resolve("query-test");
    private static String database = "query-test";

    private static RocksGrakn grakn;
    private static RocksSession session;

    @BeforeClass
    public static void setup() throws IOException {
        Util.resetDirectory(directory);
        grakn = RocksGrakn.open(directory);
        grakn.databases().create(database);
        session = grakn.session(database, Arguments.Session.Type.SCHEMA);
        try (Grakn.Transaction transaction = session.transaction(Arguments.Transaction.Type.WRITE)) {
            GraqlDefine query = Graql.parseQuery("      define\n" +
                    "      person sub entity,\n" +
                    "        plays friendship:friend,\n" +
                    "        plays employment:employee,\n" +
                    "        owns name,\n" +
                    "        owns age,\n" +
                    "        owns ref @key;\n" +
                    "      company sub entity,\n" +
                    "        plays employment:employer,\n" +
                    "        owns name,\n" +
                    "        owns ref @key;\n" +
                    "      friendship sub relation,\n" +
                    "        relates friend,\n" +
                    "        owns ref @key;\n" +
                    "      employment sub relation,\n" +
                    "        relates employee,\n" +
                    "        relates employer,\n" +
                    "        owns ref @key;\n" +
                    "      name sub attribute, value string;\n" +
                    "      age sub attribute, value long;\n" +
                    "      ref sub attribute, value long;\n" +
                    "      ship-crew sub relation, relates captain, relates navigator, relates chef, owns ref @key;\n" +
                    "      person plays ship-crew:captain, plays ship-crew:navigator, plays ship-crew:chef;");
            transaction.query().define(query);
            transaction.commit();
        }
        session.close();
        session = grakn.session(database, Arguments.Session.Type.DATA);
        try (Grakn.Transaction transaction = session.transaction(Arguments.Transaction.Type.WRITE)) {
            GraqlInsert query = Graql.parseQuery("      insert\n" +
                    "      $x isa person, has name \"Cook\", has ref 1;\n" +
                    "      $y isa person, has name \"Drake\", has ref 2;\n" +
                    "      $z isa person, has name \"Joshua\", has ref 3;\n" +
                    "      $r (captain: $x, navigator: $y, chef: $z) isa ship-crew, has ref 0;");
            transaction.query().insert(query);
            transaction.commit();
        }
    }

    @AfterClass
    public static void teardown() {
        grakn.close();
    }

    @Test
    public void test_traversal_procedure() {
        try (RocksTransaction transaction = session.transaction(READ)) {
            GraphProcedure.Builder proc = GraphProcedure.builder(4);
            Traversal.Parameters params = new Traversal.Parameters();

            ProcedureVertex.Thing r = proc.namedThing("r", true);
            r.props().types(set(Label.of("ship-crew")));
            ProcedureVertex.Thing x = proc.namedThing("x");
            x.props().types(set(Label.of("person")));
            ProcedureVertex.Thing y = proc.namedThing("y");
            y.props().types(set(Label.of("person")));
            ProcedureVertex.Type role1 = proc.namedType("role1");
            role1.props().labels(set(Label.of("captain", "ship-crew"), Label.of("navigator", "ship-crew"), Label.of("role", "relation"), Label.of("chef", "ship-crew")));
            ProcedureVertex.Thing r_role1_x_1 = proc.scopedThing(r, role1, x, 1);

            proc.forwardRelating(1, r, r_role1_x_1);
            proc.forwardIsa(2, r_role1_x_1, role1, true);
            proc.forwardRolePlayer(3, r, y, set(Label.of("captain", "ship-crew")));
            proc.backwardPlaying(4, r_role1_x_1, x);

            List<Identifier.Variable.Name> filter = Arrays.asList(
                    r.id().asVariable().asName(),
                    x.id().asVariable().asName(),
                    y.id().asVariable().asName(),
                    role1.id().asVariable().asName()
            );

            GraphProcedure procedure = proc.build();
            ResourceIterator<VertexMap> vertices = transaction.traversal().iterator(procedure, params, filter);
            assertTrue(vertices.hasNext());
            ResourceIterator<ConceptMap> answers = transaction.concepts().conceptMaps(vertices);
            assertTrue(answers.hasNext());
            ConceptMap answer;
            while (answers.hasNext()) {
                answer = answers.next();
                System.out.println(answer);
            }
        }
    }

}

