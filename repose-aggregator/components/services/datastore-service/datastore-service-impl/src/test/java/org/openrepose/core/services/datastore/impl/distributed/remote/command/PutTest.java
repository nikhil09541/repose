/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.services.datastore.impl.distributed.remote.command;

import org.junit.Assert;
import org.junit.Test;
import org.openrepose.commons.utils.http.ServiceClientResponse;
import org.openrepose.commons.utils.io.ObjectSerializer;
import org.openrepose.core.services.datastore.DatastoreOperationException;
import org.openrepose.core.services.datastore.impl.distributed.CacheRequest;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PutTest {
    static final ObjectSerializer objectSerializer = new ObjectSerializer(PutTest.class.getClassLoader());

    @Test
    public void shouldTargetCorrectPutUrl() throws UnknownHostException {
        //final Put putCommand = new Put("object-key", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000));
        final String putData = "Put data";
        final int ttl = 30;
        final String key = "someKey";
        final Put putCommand = new Put(TimeUnit.MINUTES, putData, ttl, key, new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000), null, false);

        Assert.assertEquals("Put command must target expected URL", "http://127.0.0.1:1000" + CacheRequest.CACHE_URI_PATH + key, putCommand.getUrl());
    }

    @Test
    public void shouldReturnTrueOnSuccess() throws Exception {
        final String putData = "Put data";
        final int ttl = 30;
        final Put putCommand = new Put(TimeUnit.MINUTES, putData, ttl, "somekey", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000), null, false);

        // RemoteBehavior.ALLOW_FORWARDING
        final ServiceClientResponse response = mock(ServiceClientResponse.class);
        final String responseData = "Response Data";

        ByteArrayInputStream bt = new ByteArrayInputStream(objectSerializer.writeObject(responseData));

        when(response.getData()).thenReturn(bt);
        when(response.getStatus()).thenReturn(202);

        Assert.assertEquals("Put command must communicate success on 202", Boolean.TRUE, putCommand.handleResponse(response));
    }


    @Test(expected = DatastoreOperationException.class)
    public void shouldThrowExeptionOnUnauthorized() throws Exception {
        final String putData = "Put data";
        final int ttl = 30;
        final Put putCommand = new Put(TimeUnit.MINUTES, putData, ttl, "somekey", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000), null, false);
        final ServiceClientResponse response = mock(ServiceClientResponse.class);
        when(response.getStatus()).thenReturn(HttpServletResponse.SC_UNAUTHORIZED);

        putCommand.handleResponse(response);
    }
}
