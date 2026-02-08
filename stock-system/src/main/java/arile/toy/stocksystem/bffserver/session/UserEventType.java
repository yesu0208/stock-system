package arile.toy.stocksystem.bffserver.session;

public enum UserEventType {

    CANCEL {
        @Override
        public String channel(String username) {
            return "user:cancel." + username + ":event";
        }
    },
    ORDER {
        @Override
        public String channel(String username) {
            return "user:order." + username + ":event";
        }
    },
    TRADE {
        @Override
        public String channel(String username) {
            return "user:trade." + username + ":event";
        }
    },
    ACCOUNT {
        @Override
        public String channel(String username) {
            return "user:account." + username + ":event";
        }
    },
    AUTO_ORDER {
        @Override
        public String channel(String username) {
            return "user:auto:order." + username + ":event";
        }
    },
    AUTO_CANCEL {
        @Override
        public String channel(String username) {
            return "user:auto:cancel." + username + ":event";
        }
    };

    public abstract String channel(String username);
}

