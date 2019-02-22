/*
 * GRAKN.AI - THE KNOWLEDGE GRAPH
 * Copyright (C) 2018 Grakn Labs Ltd
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

package grakn.core.server.rpc;

import grakn.core.protocol.KeyspaceProto;
import grakn.core.protocol.KeyspaceServiceGrpc;
import grakn.core.server.keyspace.Keyspace;
import grakn.core.server.keyspace.KeyspaceManager;
import grakn.core.server.session.SessionFactory;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.util.stream.Collectors;

/**
 * Grakn RPC Keyspace Service
 */
public class KeyspaceService extends KeyspaceServiceGrpc.KeyspaceServiceImplBase {

    private final KeyspaceManager keyspaceStore;
    private SessionFactory sessionFactory;

    public KeyspaceService(KeyspaceManager keyspaceStore, SessionFactory sessionFactory) {
        this.keyspaceStore = keyspaceStore;
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void create(KeyspaceProto.Keyspace.Create.Req request, StreamObserver<KeyspaceProto.Keyspace.Create.Res> response) {
        response.onError(new StatusRuntimeException(Status.UNIMPLEMENTED));
    }

    @Override
    public void retrieve(KeyspaceProto.Keyspace.Retrieve.Req request, StreamObserver<KeyspaceProto.Keyspace.Retrieve.Res> response) {
        try {
            Iterable<String> list = keyspaceStore.keyspaces().stream().map(Keyspace::getName)
                    .collect(Collectors.toSet());
            response.onNext(KeyspaceProto.Keyspace.Retrieve.Res.newBuilder().addAllNames(list).build());
            response.onCompleted();
        } catch (RuntimeException e) {
            response.onError(ResponseBuilder.exception(e));
        }
    }

    @Override
    public void delete(KeyspaceProto.Keyspace.Delete.Req request, StreamObserver<KeyspaceProto.Keyspace.Delete.Res> response) {
        try {
            Keyspace keyspace = Keyspace.of(request.getName());
            sessionFactory.deleteKeyspace(keyspace);
            keyspaceStore.deleteKeyspace(keyspace);

            response.onNext(KeyspaceProto.Keyspace.Delete.Res.getDefaultInstance());
            response.onCompleted();

        } catch (RuntimeException e) {
            response.onError(ResponseBuilder.exception(e));
        }
    }
}
