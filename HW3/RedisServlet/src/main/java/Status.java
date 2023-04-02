public class Status {
    private boolean success;
    private String description;

    public Status(boolean success, String description) {
        this.success = success;
        this.description = description;
    }

    public Status() {

    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}