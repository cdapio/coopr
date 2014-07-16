package com.continuuity.loom.store.node;

import com.continuuity.loom.account.Account;

/**
 * /**
 * Service for getting a {@link NodeStoreView} for different an account that will restrict what parts of the
 * actual store can be viewed or edited.
 */
public interface NodeStoreService {
  /**
   * Get a view of the node store as seen by the given account.
   * @param account Account of the user that is accessing the store.
   * @return View of the node store as seen by the account.
   */
  NodeStoreView getView(Account account);

  /**
   * Get the full view of the node store for system operations.
   * @return Full view of the node store.
   */
  NodeStore getSystemView();
}
