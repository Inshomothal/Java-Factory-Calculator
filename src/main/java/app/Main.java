package app;

import io.javalin.Javalin;
import java.util.*;

public class Main {
  public static void main(String[] args) {
    var app = Javalin.create(cfg -> cfg.staticFiles.add("/public")).start(8080);

    app.get("/api/recipes", ctx -> ctx.json(Domain.items()));

    app.get("/api/calc", ctx -> {
      String item = ctx.queryParam("item");
      double qty = parse(ctx.queryParam("qty"), 1.0);
      if (item == null || item.isBlank()) { ctx.status(400).result("missing item"); return; }
      if (!Domain.items().contains(item)) { ctx.status(400).result("unknown item"); return; }

      Map<String, Double> totals = Domain.compute(item, qty);
      // Present as { item: amount } with 6 decimal precision typical of fractional crafts
      var out = new TreeMap<String, Object>();
      totals.forEach((k,v) -> out.put(k, round6(v)));
      Map<String,Object> payload = Map.of(
        "item", item,
        "qty", qty,
        "totals", out
      );
      ctx.json(payload);
    });
  }

  static double parse(String s, double d){ try { return Double.parseDouble(s); } catch(Exception e){ return d; } }
  static double round6(double x){ return Math.round(x * 1_000_000.0) / 1_000_000.0; }
}
