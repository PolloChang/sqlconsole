package work.pollochang.sqlconsole.model.dto;

import lombok.Data;

@Data
public class ConnectionRequest {
    private Long driverId;
    private String url;
    private String username;
    private String password;
}
