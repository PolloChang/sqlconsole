package work.pollochang.sqlconsole.service.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import work.pollochang.sqlconsole.model.dto.SqlResult;
import work.pollochang.sqlconsole.service.AuditService;

import java.util.List;

// @ConditionalOnMissingBean 意思是：如果沒有別人 (付費版) 搶著做，我就做。
@Service
public class FreeAuditService implements AuditService {

    @Override
    public SqlResult checkAndAudit(String username, Long dbId, String sql) {
        // 免費版沒有審核功能，永遠回傳 null (代表 Pass，直接執行)
        return null;
    }

    @Override
    public SqlResult executeApprovedTask(Long taskId, String auditorName) {
        return new SqlResult("ERROR", "COMMITTED", "此功能僅限企業版使用", null, null);
    }

    @Override
    public List<?> getPendingTasks() {
        // 免費版沒有審核功能，回傳空清單
        return java.util.Collections.emptyList();
    }
}