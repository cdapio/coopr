package com.continuuity.loom.store.provisioner;

import com.continuuity.loom.account.Account;
import com.continuuity.loom.provisioner.PluginResourceType;
import com.google.common.util.concurrent.Service;

/**
 *
 */
public interface PluginResourceMetaStoreService extends Service {

  PluginResourceMetaStoreView getView(Account account, PluginResourceType type);
}
