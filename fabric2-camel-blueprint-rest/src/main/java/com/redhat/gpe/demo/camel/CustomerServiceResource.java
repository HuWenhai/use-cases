/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.redhat.gpe.demo.camel;

import com.redhat.gpe.demo.camel.model.Customer;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("/customerservice/")
@Produces({"application/xml", "application/json"})
public class CustomerServiceResource {

    public CustomerServiceResource() {
    }

    @GET
    @Path("/customers/{id}/")
    public Response getCustomer(@PathParam("id") String id) {
        return null;
    }

    @PUT
    @Path("/customers/")
    @Consumes({"application/xml", "application/json"})
    public Response updateCustomer(Customer customer) {
        return null;
    }

    @POST
    @Path("/customers/")
    @Consumes({"application/xml", "application/json"})
    public Response addCustomer(Customer customer) {
        return null;
    }

    @DELETE
    @Path("/customers/{id}/")
    public Response deleteCustomer(@PathParam("id") String id) {
        return null;
    }
}