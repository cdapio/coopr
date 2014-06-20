package com.continuuity.loom.codec.json.upgrade;

import com.continuuity.loom.admin.Provider;
import com.continuuity.loom.admin.Service;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.codec.json.BaseJsonSerde;
import com.google.gson.Gson;

/**
 * Codec that upgrades objects from 0.9.6 and 0.9.7 to 0.9.8.
 */
public class UpgradeJsonSerde extends BaseJsonSerde {
  private final Gson gson;

  public UpgradeJsonSerde(String tenantId) {
    this.gson = createCurrentBuilder()
      .registerTypeAdapter(Cluster.class, new ClusterUpgradeCodec(tenantId))
      .registerTypeAdapter(Provider.class, new ProviderUpgradeCodec())
      .registerTypeAdapter(Service.class, new ServiceUpgradeCodec())
      .create();
  }

  @Override
  public Gson getGson() {
    return gson;
  }
}
