package co.cask.coopr.store.entity;

import co.cask.coopr.account.Account;
import com.google.common.util.concurrent.Service;

import java.io.IOException;

/**
 * Service that returns views of the entity store as seen by tenant admins.
 */
public interface EntityStoreService extends Service {

  /**
   * Get a view of the entity store as seen by the given account.
   *
   * @param account Account that will be viewing the entity store
   * @return view of the entity store as seen by the given account.
   */
  EntityStoreView getView(Account account);

  /**
   * Copy all entities from one account to another. Overwrites existing entities in target account if they already
   * exist. This operation is not atomic, and should be used only in specific circumstances, such as when bootstrapping
   * a new, empty account.
   *
   * @param from Account to copy from
   * @param to Account to copy to
   * @throws IOException if there was a problem copying the entities
   */
  void copyEntities(Account from, Account to) throws IOException, IllegalAccessException;
}
