package co.cask.coopr.cluster;

import co.cask.coopr.spec.plugin.FieldSchema;

import java.util.List;
import java.util.Map;

/**
 * An exception indicating there are required fields that are missing.
 */
public class MissingFieldsException extends Exception {
  private final List<Map<String, FieldSchema>> missingFields;

  public MissingFieldsException(List<Map<String, FieldSchema>> missingFields) {
    this.missingFields = missingFields;
  }

  public List<Map<String, FieldSchema>> getMissingFields() {
    return missingFields;
  }
}
