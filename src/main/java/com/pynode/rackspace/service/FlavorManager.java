package com.pynode.rackspace.service;

import com.rackspace.cloud.api.Flavor;

/**
 * create, remove and update will raise BadMethodFaults
 * @author Christos Fragoulides
 */
public interface FlavorManager extends EntityManager<Flavor> { }
