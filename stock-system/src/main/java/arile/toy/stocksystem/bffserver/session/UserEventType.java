package arile.toy.stocksystem.bffserver.session;

public enum UserEventType {
    
    ORDER {
        @Override
        public String channel(String username) {
            return "user:order." + username + ":event";
        }
    },
    ACCOUNT {
        @Override
        public String channel(String username) {
            return "user:account." + username + ":event";
        }
    },
    CANCEL {
        @Override
        public String channel(String username) {
            return "user:cancel." + username + ":event";
        }
    };

    public abstract String channel(String username);
}

