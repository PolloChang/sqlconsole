package com.sqlconsole.core.service;

import com.sqlconsole.core.model.enums.DbType;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Fetch;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.Offset;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SqlPaginationService {

  public String applyPagination(String originalSql, DbType dbType, int page, int size) {
    try {
      Statement statement = CCJSqlParserUtil.parse(originalSql);
      if (!(statement instanceof Select)) {
        return originalSql;
      }

      Select select = (Select) statement;
      if (!(select.getSelectBody() instanceof PlainSelect)) {
        // Unsupported complex query (e.g. UNION), fallback to original
        return originalSql;
      }

      PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
      long offset = (long) (page - 1) * size;

      switch (dbType) {
        case MYSQL:
        case POSTGRESQL:
        case H2:
          Limit limit = new Limit();
          limit.setRowCount(new LongValue(size));
          plainSelect.setLimit(limit);

          if (offset > 0) {
            Offset offsetObj = new Offset();
            offsetObj.setOffset(new LongValue(offset));
            plainSelect.setOffset(offsetObj);
          }
          break;

        case ORACLE:
        case MSSQL:
          if (plainSelect.getOrderByElements() == null) {
            // OFFSET/FETCH usually requires ORDER BY in these dialects
            // We cannot safely inject an ORDER BY without knowing the schema.
            // Throw exception to trigger fallback.
            throw new JSQLParserException("ORDER BY required for OFFSET/FETCH pagination");
          }
          Offset offsetClause = new Offset();
          offsetClause.setOffset(new LongValue(offset));
          offsetClause.setOffsetParam("ROWS");
          plainSelect.setOffset(offsetClause);

          Fetch fetch = new Fetch();
          fetch.setRowCount(size);
          fetch.setFetchParam("ROWS ONLY");
          plainSelect.setFetch(fetch);
          break;

        default:
          // Other dialects: Fallback
          return originalSql;
      }

      return select.toString();

    } catch (JSQLParserException e) {
      log.warn("Failed to parse/rewrite SQL for pagination: {}", e.getMessage());
      return originalSql;
    }
  }
}
