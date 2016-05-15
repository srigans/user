/**
 *  Copyright 2005-2015 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package com.gans.user;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Context;
import java.util.HashMap;
import java.util.Map;

/**
 * This Java class with be hosted in the URI path defined by the @Path annotation. @Path annotations on the methods
 * of this class always refer to a path relative to the path defined at the class level.
 * <p/>
 * For example, with 'http://localhost:8080/' as the default CXF servlet path and '/' as the JAX-RS server path,
 * this class will be hosted in 'http://localhost:8080/userservice/'.  An @Path("/users") annotation on
 * one of the methods would result in 'http://localhost:8080/userservice/users'.
 */
@Path("/")
@Api(value = "/", description = "Operations for users")

public class UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    long currentId = 123;
    // replace this with MySQL DB
    Map<Long, User> users = new HashMap<Long, User>();
    private MessageContext jaxrsContext;

    public UserService() {
        init();
    }

    /**
     * This method is mapped to an HTTP GET of 'http://localhost:8080/userservice/users/{id}'.  The value for
     * {id} will be passed to this message as a parameter, using the @PathParam annotation.
     * <p/>
     * The method returns a User object - for creating the HTTP response, this object is marshaled into XML using JAXB.
     * <p/>
     * For example: surfing to 'http://localhost:8080/userservice/users/123' will show you the information of
     * user 123 in XML format.
     */
    @GET
    @Path("/users/{id}/")
    @Produces("application/xml")
    @ApiOperation(value = "Find User by ID", notes = "More notes about this method", response = User.class)
    @ApiResponses(value = {
      @ApiResponse(code = 500, message = "Invalid ID supplied"),
      @ApiResponse(code = 204, message = "User not found") 
    })
    public User getUser(@ApiParam(value = "ID of User to fetch", required = true) @PathParam("id") String id) {
        LOG.info("Invoking getUser, User id is: {}", id);
        long idNumber = Long.parseLong(id);
        User c = users.get(idNumber);
        return c;
    }

    /**
     * Using HTTP PUT, we can can upload the XML representation of a user object.  This operation will be mapped
     * to the method below and the XML representation will get unmarshaled into a real User object using JAXB.
     * <p/>
     * The method itself just updates the user object in our local data map and afterwards uses the Reponse class to
     * build the appropriate HTTP response: either OK if the update succeeded (translates to HTTP Status 200/OK) or not
     * modified if the method failed to update a user object (translates to HTTP Status 304/Not Modified).
     * <p/>
     * Note how this method is using the same @Path value as our next method - the HTTP method used will determine which
     * method is being invoked.
     */
    @PUT
    @Path("/users/")
    @Consumes({"application/xml", "application/json" })
    @ApiOperation(value = "Update an existing User")
    @ApiResponses(value = {
                           @ApiResponse(code = 500, message = "Invalid ID supplied"),
                           @ApiResponse(code = 204, message = "User not found") 
                         })
    public Response updateUser(@ApiParam(value = "User object that needs to be updated", required = true) User user) {
        LOG.info("Invoking updateUser, User name is: {}", user.getUserName());
        User c = users.get(user.getId());
        Response r;
        if (c != null) {
            users.put(user.getId(), user);
            r = Response.ok().build();
        } else {
            r = Response.notModified().build();
        }

        return r;
    }

    /**
     * Using HTTP POST, we can add a new user to the system by uploading the XML representation for the user.
     * This operation will be mapped to the method below and the XML representation will get unmarshaled into a real
     * User object.
     * <p/>
     * After the method has added the user to the local data map, it will use the Response class to build the HTTP reponse,
     * sending back the inserted user object together with a HTTP Status 200/OK.  This allows us to send back the
     * new id for the user object to the client application along with any other data that might have been updated in
     * the process.
     * <p/>
     * Note how this method is using the same @Path value as our previous method - the HTTP method used will determine which
     * method is being invoked.
     */
    @POST
    @Path("/users/")
    @Consumes({"application/xml", "application/json" })
    @ApiOperation(value = "Add a new User")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Invalid ID supplied"), })
    public Response createUser(@ApiParam(value = "User object that needs to be updated", required = true)
                                User user) {
        LOG.info("Invoking addUser, User name is: {}", user.getUserName());
        user.setId(++currentId);
         
        users.put(user.getId(), user);
        if (jaxrsContext != null && jaxrsContext.getHttpHeaders().getMediaType().getSubtype().equals("json")) {
            return Response.ok().type("application/json").entity(user).build();
        } else {
            return Response.ok().type("application/xml").entity(user).build();
        }
    }

    /**
     * This method is mapped to an HTTP DELETE of 'http://localhost:8080/userservice/users/{id}'.  The value for
     * {id} will be passed to this message as a parameter, using the @PathParam annotation.
     * <p/>
     * The method uses the Response class to create the HTTP response: either HTTP Status 200/OK if the user object was
     * successfully removed from the local data map or a HTTP Status 304/Not Modified if it failed to remove the object.
     */
    @DELETE
    @Path("/users/{id}/")
    @ApiOperation(value = "Delete User")
    @ApiResponses(value = {
                           @ApiResponse(code = 500, message = "Invalid ID supplied"),
                           @ApiResponse(code = 204, message = "User not found") 
                         })
    public Response deleteUser(@ApiParam(value = "ID of User to delete", required = true) @PathParam("id") String id) {
        LOG.info("Invoking deleteUser, User id is: {}", id);
        long idNumber = Long.parseLong(id);
        User c = users.get(idNumber);

        Response r;
        if (c != null) {
            r = Response.ok().build();
            users.remove(idNumber);
        } else {
            r = Response.notModified().build();
        }

        return r;
    }

    /**
     * The init method is used by the constructor to insert a User and Order object into the local data map
     * for testing purposes.
     */
    final void init() {
        /*User c = new User();
        c.setName("John");
        c.setId(123);
        users.put(c.getId(), c);*/
    }
    
    @Context
    public void setMessageContext(MessageContext messageContext) {
        this.jaxrsContext = messageContext;
    }
}
