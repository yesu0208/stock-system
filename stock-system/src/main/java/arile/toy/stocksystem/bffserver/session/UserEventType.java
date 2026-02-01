package arile.toy.stocksystem.bffserver.session;

public enum UserEventType {

    ORDER {
        @Override
        public String channel(String username) {
            return "user:order." + username + ":event";
        }
    };

    public abstract String channel(String username);
}

