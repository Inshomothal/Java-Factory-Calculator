package app;

import java.util.*;

/** Minimal recipe engine with byproducts and a 1 Barrel = 10000 Leeks conversion. */
public class Domain {

  /** An input needed per craft. */
  public record Ingredient(String name, double qty) {}

  /** A recipe: inputs -> 1 unit output, with optional byproducts (credited). */
  public static final class Recipe {
    public final String name;
    public final List<Ingredient> inputs;
    public final Map<String, Double> byproducts; // credited per craft (can be empty)

    public Recipe(String name, List<Ingredient> inputs, Map<String, Double> byproducts) {
      this.name = name;
      this.inputs = List.copyOf(inputs);
      this.byproducts = byproducts == null ? Map.of() : Map.copyOf(byproducts);
    }
  }

  /** Recipe book. NOTE: "Barrel of Leeks" is craftable from 10000 Leeks (conversion). */
  public static final Map<String, Recipe> BOOK = new HashMap<>();
  static {
    // Base resource: Leeks (no recipe = gathered)
    // Conversion: 1 Barrel of Leeks = 10000 Leeks
    BOOK.put("Barrel of Leeks", new Recipe(
      "Barrel of Leeks",
      List.of(new Ingredient("Leeks", 10000)),
      Map.of()
    ));

    // Leek Teas = 10 Leeks
    BOOK.put("Leek Teas", new Recipe(
      "Leek Teas",
      List.of(new Ingredient("Leeks", 10)),
      Map.of()
    ));

    // Leek Cakes = 10 Leek Teas
    BOOK.put("Leek Cakes", new Recipe(
      "Leek Cakes",
      List.of(new Ingredient("Leek Teas", 10)),
      Map.of()
    ));

    // Atomic Leeks = 10 Leek Cakes, 25 Leek Teas
    BOOK.put("Atomic Leeks", new Recipe(
      "Atomic Leeks",
      List.of(new Ingredient("Leek Cakes", 10), new Ingredient("Leek Teas", 25)),
      Map.of()
    ));

    // Golden Leeks (byproduct: 500 Leek Teas) = 10 Atomic Leeks, 50 Leek Cakes, 0.2 Barrel
    BOOK.put("Golden Leeks", new Recipe(
      "Golden Leeks",
      List.of(
        new Ingredient("Atomic Leeks", 10),
        new Ingredient("Leek Cakes", 50),
        new Ingredient("Barrel of Leeks", 0.2)
      ),
      Map.of("Leek Teas", 500.0)
    ));

    // Ultraleeks (byproduct: 100000 Leeks) = 10 Golden, 20 Atomic, 1000 Teas, 0.4 Barrel
    BOOK.put("Ultraleeks", new Recipe(
      "Ultraleeks",
      List.of(
        new Ingredient("Golden Leeks", 10),
        new Ingredient("Atomic Leeks", 20),
        new Ingredient("Leek Teas", 1000),
        new Ingredient("Barrel of Leeks", 0.4)
      ),
      Map.of("Leeks", 100000.0)
    ));
  }

  /** Compute net base requirements to produce `qtyOut` of `item`. */
  public static Map<String, Double> compute(String item, double qtyOut) {
    if (qtyOut < 0) throw new IllegalArgumentException("qtyOut must be >= 0");
    Map<String, Double> totals = new TreeMap<>();
    // Positive means "require", negative means "surplus/credit" from byproducts
    expand(item, qtyOut, totals, new ArrayDeque<>());
    // Clean small negatives/floating noise
    clampNegligible(totals, 1e-9);
    // If an item is both needed and credited, net them out
    return totals;
  }

  private static void expand(String item, double qty, Map<String, Double> acc, Deque<String> stack) {
    if (qty <= 0) return;
    if (stack.contains(item)) throw new IllegalStateException("Cycle: " + stack + " -> " + item);
    stack.push(item);

    Recipe r = BOOK.get(item);
    if (r == null) {
      // Base/gathered resource (e.g., Leeks). Just require it.
      acc.merge(item, qty, Double::sum);
      stack.pop();
      return;
    }

    // For 1 unit output, consume inputs and credit byproducts.
    // For qty units, scale linearly.
    for (Ingredient in : r.inputs) {
      double need = in.qty * qty;
      expand(in.name, need, acc, stack);
    }
    for (var bp : r.byproducts.entrySet()) {
      double credit = bp.getValue() * qty;
      acc.merge(bp.getKey(), -credit, Double::sum); // subtract need (credit)
    }

    // Also count the produced item itself: if something upstream needs it, this ensures
    // its machines would be counted later. For now, we only track requirements/credits.
    acc.merge(item, 0.0, Double::sum); // no-op, but keeps key visible

    stack.pop();
  }

  private static void clampNegligible(Map<String, Double> map, double eps) {
    List<String> toRemove = new ArrayList<>();
    for (var e : map.entrySet()) {
      double v = e.getValue();
      if (Math.abs(v) < eps) toRemove.add(e.getKey());
    }
    toRemove.forEach(map::remove);
  }

  /** Convenience: expose available craftable items for the UI (sorted). */
  public static List<String> items() {
    // Allow crafting any known output plus "Leeks" base if user wants to compute nothing (harvest).
    Set<String> names = new HashSet<>(BOOK.keySet());
    names.add("Leeks");
    return names.stream().sorted().toList();
  }
}
