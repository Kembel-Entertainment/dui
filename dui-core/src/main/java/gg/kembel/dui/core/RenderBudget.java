package gg.kembel.dui.core;

import java.util.*;

/** Allocate scarce renderer resources: minimums first, then remaining demand in caller order. */
public final class RenderBudget {
  public record Request(String id, int demand, int minimum) {
    public Request {
      if (id == null || id.isBlank() || minimum < 0 || demand < minimum)
        throw new IllegalArgumentException(
            "Resource request needs an id and 0 <= minimum <= demand");
    }
  }

  private RenderBudget() {}

  public static Map<String, Integer> allocate(int capacity, Request... requests) {
    if (capacity < 0) throw new IllegalArgumentException("Resource capacity must be nonnegative");
    var allocation = new LinkedHashMap<String, Integer>();
    long minimum = 0;
    for (var request : requests) {
      if (allocation.putIfAbsent(request.id, request.minimum) != null)
        throw new IllegalArgumentException("Duplicate resource group: " + request.id);
      minimum += request.minimum;
    }
    if (minimum > capacity)
      throw new IllegalArgumentException(
          "Resource minimums " + allocation + " exceed capacity " + capacity);
    int remaining = capacity - (int) minimum;
    for (var request : requests) {
      int extra = Math.min(remaining, request.demand - request.minimum);
      allocation.put(request.id, request.minimum + extra);
      remaining -= extra;
    }
    return Collections.unmodifiableMap(allocation);
  }
}
