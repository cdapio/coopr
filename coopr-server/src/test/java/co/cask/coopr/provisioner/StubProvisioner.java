package co.cask.coopr.provisioner;

import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.scheduler.callback.DummyService;
import co.cask.http.AbstractHttpHandler;
import co.cask.http.HttpResponder;
import co.cask.http.NettyHttpService;
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
  private Multiset<String> tenantWorkerPutCounts;
  private Multiset<String> tenantResourcePutCounts;
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
    this.tenantResourcePutCounts = HashMultiset.create();
    this.tenantWorkerPutCounts = HashMultiset.create();
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

  public int getPutWorkersCount(String tenantId) {
    return tenantWorkerPutCounts.count(tenantId);
  }

  public int getPutResourcesCount(String tenantId) {
    return tenantResourcePutCounts.count(tenantId);
  }

  public int getDeleteCount(String tenantId) {
    return tenantDeleteCounts.count(tenantId);
  }

  public void reset() {
    tenantPutCounts.clear();
    tenantWorkerPutCounts.clear();
    tenantResourcePutCounts.clear();
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

  @Path(Constants.API_BASE)
  public class StubTenantHandler extends AbstractHttpHandler {

    @PUT
    @Path("/tenants/{tenant-id}")
    public void putTenant(HttpRequest request, HttpResponder responder, @PathParam("tenant-id") String tenantId) {
      tenantPutCounts.add(tenantId);
      responder.sendStatus(HttpResponseStatus.OK);
    }

    @PUT
    @Path("/tenants/{tenant-id}/workers")
    public void putTenantWorkers(HttpRequest request, HttpResponder responder,
                                 @PathParam("tenant-id") String tenantId) {
      tenantWorkerPutCounts.add(tenantId);
      responder.sendStatus(HttpResponseStatus.OK);
    }

    @PUT
    @Path("/tenants/{tenant-id}/resources")
    public void putTenantResources(HttpRequest request, HttpResponder responder,
                                   @PathParam("tenant-id") String tenantId) {
      tenantResourcePutCounts.add(tenantId);
      responder.sendStatus(HttpResponseStatus.OK);
    }

    @DELETE
    @Path("/tenants/{tenant-id}")
    public void deleteTenant(HttpRequest request, HttpResponder responder, @PathParam("tenant-id") String tenantId) {
      tenantDeleteCounts.add(tenantId);
      responder.sendStatus(HttpResponseStatus.OK);
    }
  }
}
