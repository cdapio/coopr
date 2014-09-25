package co.cask.coopr.store.cluster;

import co.cask.coopr.account.Account;
import com.google.common.util.concurrent.Service;

/**
 * Service for getting a {@link ClusterStoreView} for different an account that will restrict what parts of the
 * actual store can be viewed or edited.
 */
public interface ClusterStoreService extends Service {

  /**
   * Get a view of the cluster store as seen by the given account.
   *
   * @param account Account of the user that is accessing the store.
   * @return View of the cluster store as seen by the account.
   */
  ClusterStoreView getView(Account account);

  /**
   * Get the full view of the cluster store for system operations.
   *
   * @return Full view of the cluster store.
   */
  ClusterStore getSystemView();
}
