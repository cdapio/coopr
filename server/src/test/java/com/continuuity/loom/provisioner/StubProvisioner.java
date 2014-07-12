package com.continuuity.loom.provisioner;

import com.continuuity.http.AbstractHttpHandler;
import com.continuuity.http.HttpResponder;
import com.continuuity.http.NettyHttpService;
import com.continuuity.loom.scheduler.callback.DummyService;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.util.concurrent.AbstractIdleService;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.net.InetSocketAddress;

/**
 *
 */
public class StubProvisioner extends AbstractIdleService {
  private static final Logger LOG  = LoggerFactory.getLogger(DummyService.class);
  private final NettyHttpService httpService;
  private Multiset<String> tenantPutCounts;
  private Multiset<String> tenantDeleteCounts;

  public StubProvisioner() {
    NettyHttpService.Builder builder = NettyHttpService.builder();
    builder.addHttpHandlers(ImmutableSet.of(new StubTenantHandler()));

    builder.setHost("localhost");
    builder.setPort(0);

    builder.setConnectionBacklog(20000);
    builder.setExecThreadPoolSize(1);
    builder.setBossThreadPoolSize(1);
    builder.setWorkerThreadPoolSize(1);

    this.httpService = builder.build();
    this.tenantPutCounts = HashMultiset.create();
    this.tenantDeleteCounts = HashMultiset.create();
  }

  @Override
  protected void startUp() throws Exception {
    httpService.startAndWait();
    LOG.info("Dummy service started successfully on {}", httpService.getBindAddress());
  }

  @Override
  protected void shutDown() throws Exception {
    httpService.stopAndWait();
  }

  public int getPutCount(String tenantId) {
    return tenantPutCounts.count(tenantId);
  }

  public int getDeleteCount(String tenantId) {
    return tenantDeleteCounts.count(tenantId);
  }

  public void reset() {
    tenantPutCounts.clear();
    tenantDeleteCounts.clear();
  }

  /**
   * Get the address the service has bound to.
   *
   * @return Address the service has bound to.
   */
  public InetSocketAddress getBindAddress() {
    return httpService.getBindAddress();
  }

  public class StubTenantHandler extends AbstractHttpHandler {

    @PUT
    @Path("/v1/tenants/{tenant-id}")
    public void putTenant(HttpRequest request, HttpResponder responder, @PathParam("tenant-id") String tenantId) {
      tenantPutCounts.add(tenantId);
      responder.sendStatus(HttpResponseStatus.OK);
    }

    @DELETE
    @Path("/v1/tenants/{tenant-id}")
    public void deleteTenant(HttpRequest request, HttpResponder responder, @PathParam("tenant-id") String tenantId) {
      tenantDeleteCounts.add(tenantId);
      responder.sendStatus(HttpResponseStatus.OK);
    }
  }
}
