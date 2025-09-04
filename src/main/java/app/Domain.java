package app;

import java.util.*;

public class Domain {

  public record Ingredient(String name, double qty) {}

  public static final class Recipe {
    public final String name;
    public final List<Ingredient> inputs;
    public final Map<String, Double> byproducts;
    public Recipe(String name, List<Ingredient> inputs, Map<String, Double> byproducts) {
      this.name = name;
      this.inputs = List.copyOf(inputs);
      this.byproducts = byproducts == null ? Map.of() : Map.copyOf(byproducts);
    }
  }

  // ---------- Recipes ----------
  public static final Map<String, Recipe> BOOK = new HashMap<>();
  static {
    BOOK.put("Barrel of Leeks", new Recipe(
      "Barrel of Leeks", List.of(new Ingredient("Leeks", 10000)), Map.of()
    ));
    BOOK.put("Leek Teas", new Recipe("Leek Teas", List.of(new Ingredient("Leeks", 10)), Map.of()));
    BOOK.put("Leek Cakes", new Recipe("Leek Cakes", List.of(new Ingredient("Leek Teas", 10)), Map.of()));
    BOOK.put("Atomic Leeks", new Recipe("Atomic Leeks",
      List.of(new Ingredient("Leek Cakes", 10), new Ingredient("Leek Teas", 25)), Map.of()));
    BOOK.put("Golden Leeks", new Recipe("Golden Leeks",
      List.of(new Ingredient("Atomic Leeks", 10), new Ingredient("Leek Cakes", 50), new Ingredient("Barrel of Leeks", 0.2)),
      Map.of("Leek Teas", 500.0)));
    BOOK.put("Ultraleeks", new Recipe("Ultraleeks",
      List.of(new Ingredient("Golden Leeks", 10), new Ingredient("Atomic Leeks", 20),
              new Ingredient("Leek Teas", 1000), new Ingredient("Barrel of Leeks", 0.4)),
      Map.of("Leeks", 100000.0)));
  }

  /** Plan = what to craft (crafts), base needs (base), and credits from byproducts (credits). */
  public record Plan(Map<String,Double> crafts, Map<String,Double> base, Map<String,Double> credits) {}

  /** Compute full plan to produce qtyOut of item (acyclic expansion). */
  public static Plan computePlan(String item, double qtyOut) {
    Map<String,Double> crafts = new TreeMap<>();
    Map<String,Double> base = new TreeMap<>();
    Map<String,Double> credits = new TreeMap<>();
    expand(item, qtyOut, crafts, base, credits, new ArrayDeque<>());
    // net base totals = base - credits
    return new Plan(crafts, base, credits);
  }

  /** Convenience: net base totals (base - credits). */
  public static Map<String,Double> netBaseTotals(Plan p) {
    Map<String,Double> out = new TreeMap<>();
    for (var e : p.base.entrySet()) out.merge(e.getKey(), e.getValue(), Double::sum);
    for (var e : p.credits.entrySet()) out.merge(e.getKey(), -e.getValue(), Double::sum);
    // remove ~zero noise
    clamp(out, 1e-9);
    return out;
  }

  private static void expand(String item, double qty,
                             Map<String,Double> crafts,
                             Map<String,Double> base,
                             Map<String,Double> credits,
                             Deque<String> stack) {
    if (qty <= 0) return;
    if (stack.contains(item)) throw new IllegalStateException("Cycle: "+stack+" -> "+item);
    stack.push(item);

    Recipe r = BOOK.get(item);
    if (r == null) {
      base.merge(item, qty, Double::sum);
      stack.pop(); return;
    }

    // we will craft this item
    crafts.merge(item, qty, Double::sum);

    // consume inputs
    for (Ingredient in : r.inputs) {
      expand(in.name, qty * in.qty, crafts, base, credits, stack);
    }
    // credit byproducts
    for (var bp : r.byproducts.entrySet()) {
      credits.merge(bp.getKey(), qty * bp.getValue(), Double::sum);
    }

    stack.pop();
  }

  private static void clamp(Map<String,Double> m, double eps) {
    List<String> rm = new ArrayList<>();
    for (var e : m.entrySet()) if (Math.abs(e.getValue()) < eps) rm.add(e.getKey());
    rm.forEach(m::remove);
  }

  public static List<String> items() {
    Set<String> names = new HashSet<>(BOOK.keySet());
    names.add("Leeks"); // base
    return names.stream().sorted().toList();
  }
}
