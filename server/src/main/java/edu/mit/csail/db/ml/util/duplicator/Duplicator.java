package edu.mit.csail.db.ml.util.duplicator;


import jooq.sqlite.gen.tables.records.DataframeRecord;
import org.jooq.DSLContext;
import org.jooq.Query;

import java.util.*;

public abstract class Duplicator<T> {
  protected Map<Integer, List<Integer>> idsForOriginalId;
  protected Map<Integer, T> recForOriginalId;
  protected DSLContext ctx;
  protected int maxId;
  int numBuffered;

  void duplicate(int numIterations) {
    resetQuery();

    Set<Integer> keys = idsForOriginalId.keySet();
    for (int id : keys) {
      T rec = recForOriginalId.get(id);
      for (int i = 1; i < numIterations; i++) {
        maxId++;
        updateQuery(rec, i);
        processed(id, maxId);
        if (tryExecute(getQuery())) {
          resetQuery();
        }
      }
    }
    forceExecute(getQuery());
  }

  abstract protected Query getQuery();

  abstract protected void resetQuery();

  abstract protected void updateQuery(T rec, int iteration);

  protected void init(DSLContext ctx) {
    numBuffered = 0;
    idsForOriginalId = new HashMap<>();
    maxId= -1;
    recForOriginalId = new HashMap<>();
    this.ctx = ctx;
  }

  protected boolean tryExecute(Query q) {
    if (numBuffered < 100) {
      numBuffered++;
      return false;
    } else {
      q.execute();
      numBuffered = 0;
      return true;
    }
  }

  protected void forceExecute(Query q) {
    if (numBuffered > 0) {
      q.execute();
    }
  }

  protected void updateMaps(int id, T rec) {
    ArrayList<Integer> l = new ArrayList<>();
    l.add(id);
    idsForOriginalId.put(id, l);
    recForOriginalId.put(id, rec);
    maxId = Math.max(id, maxId);
  }

  protected void processed(int origId, int id) {
    idsForOriginalId.get(origId).add(id);
  }

  public int id(int originalId, int iteration) {
    return idsForOriginalId.get(originalId).get(iteration);
  }
}
