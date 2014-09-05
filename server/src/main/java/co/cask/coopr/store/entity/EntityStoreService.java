package co.cask.coopr.store.entity;

import co.cask.coopr.account.Account;
import com.google.common.util.concurrent.Service;

/**
 * Service that returns views of the entity store as seen by tenant admins.
 */
public interface EntityStoreService extends Service {

  EntityStoreView getView(Account account);
}
