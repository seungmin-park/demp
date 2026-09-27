package com.inhatc.demp.support;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.resource.jdbc.spi.StatementInspector;

public class SqlCaptureInspector implements StatementInspector {
    private static final ThreadLocal<List<String>> STATEMENTS = ThreadLocal.withInitial(ArrayList::new);

    @Override
    public String inspect(String sql) {
        STATEMENTS.get().add(sql);
        return sql;
    }

    public static void clear() {
        STATEMENTS.get().clear();
    }

    public static List<String> statements() {
        return List.copyOf(STATEMENTS.get());
    }
}
