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
      if (item == null || !Domain.items().contains(item)) { ctx.status(400).result("unknown item"); return; }

      var plan = Domain.computePlan(item, qty);
      var totals = Domain.netBaseTotals(plan);

      Map<String,Object> payload = new LinkedHashMap<>();
      payload.put("item", item);
      payload.put("qty", qty);
      payload.put("crafts", roundMap(plan.crafts()));
      payload.put("base", roundMap(plan.base()));
      payload.put("credits", roundMap(plan.credits()));
      payload.put("totals", roundMap(totals)); // base - credits
      ctx.json(payload);
    });
  }

  static double parse(String s, double d){ try { return Double.parseDouble(s); } catch(Exception e){ return d; } }
  static Map<String,Double> roundMap(Map<String,Double> in){
    Map<String,Double> m = new TreeMap<>();
    in.forEach((k,v)-> m.put(k, Math.round(v*1_000_000.0)/1_000_000.0));
    return m;
  }
}
