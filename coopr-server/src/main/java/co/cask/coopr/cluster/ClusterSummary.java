package co.cask.coopr.cluster;

import co.cask.coopr.scheduler.task.ClusterJob;
import co.cask.coopr.spec.BaseEntity;
import co.cask.coopr.spec.NamedEntity;
import co.cask.coopr.spec.Provider;
import co.cask.coopr.spec.template.ClusterTemplate;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Summary of a {@link Cluster}, used in responding to REST API calls.
 */
public class ClusterSummary {
  private final String id;
  private final String name;
  private final String description;
  private final String ownerId;
  private final long createTime;
  private final long expireTime;
  private final int numNodes;
  private final Cluster.Status status;
  // provider and template are named entities to keep the structure of a provider and template consistent
  // between different api calls, when the full provider or template may be returned.
  private final BaseEntity provider;
  private final BaseEntity clusterTemplate;
  private final Set<String> services;
  private final ClusterJobProgress progress;

  public ClusterSummary(Cluster cluster, ClusterJob clusterJob) {
    this.id = cluster.getId();
    this.name = cluster.getName();
    this.description = cluster.getDescription();
    this.ownerId = cluster.getAccount().getUserId();
    this.createTime = cluster.getCreateTime();
    this.expireTime = cluster.getExpireTime();
    Provider provider = cluster.getProvider();
    this.provider = provider == null ? null : BaseEntity.from(provider);
    ClusterTemplate clusterTemplate = cluster.getClusterTemplate();
    this.clusterTemplate = clusterTemplate == null ? null : BaseEntity.from(clusterTemplate);
    this.numNodes = cluster.getNodeIDs().size();
    this.status = cluster.getStatus();
    this.services = ImmutableSet.copyOf(cluster.getServices());
    this.progress = new ClusterJobProgress(clusterJob);
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public long getCreateTime() {
    return createTime;
  }

  public long getExpireTime() {
    return expireTime;
  }

  public int getNumNodes() {
    return numNodes;
  }

  public Cluster.Status getStatus() {
    return status;
  }

  public NamedEntity getProvider() {
    return provider;
  }

  public NamedEntity getClusterTemplate() {
    return clusterTemplate;
  }

  public Set<String> getServices() {
    return services;
  }

  public ClusterJobProgress getProgress() {
    return progress;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ClusterSummary that = (ClusterSummary) o;

    return Objects.equal(id, that.id) &&
      Objects.equal(name, that.name) &&
      Objects.equal(description, that.description) &&
      Objects.equal(ownerId, that.ownerId) &&
      createTime == that.createTime &&
      expireTime == that.expireTime &&
      numNodes == that.numNodes &&
      status == that.status &&
      Objects.equal(clusterTemplate, that.clusterTemplate) &&
      Objects.equal(provider, that.provider) &&
      Objects.equal(services, that.services) &&
      Objects.equal(progress, that.progress);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id, name, description, ownerId, createTime, expireTime,
                            numNodes, status, clusterTemplate, provider, services, progress);
  }
}
