import java.util.Objects;

public class Swipe {

    private String swipe;
    private String swiper;
    private String swipee;
    private String comment;

    public Swipe(String swipe, String swiper, String swipee, String comment) {
        this.swipe = swipe;
        this.swiper = swiper;
        this.swipee = swipee;
        this.comment = comment;
    }

    public String getSwipe() {
        return swipe;
    }

    public void setSwipe(String swipe) {
        this.swipe = swipe;
    }

    public String getSwiper() {
        return swiper;
    }

    public void setSwiper(String swiper) {
        this.swiper = swiper;
    }

    public String getSwipee() {
        return swipee;
    }

    public void setSwipee(String swipee) {
        this.swipee = swipee;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Swipe swipe1 = (Swipe) o;
        return Objects.equals(swipe, swipe1.swipe)
                && Objects.equals(swiper, swipe1.swiper)
                && Objects.equals(swipee, swipe1.swipee)
                && Objects.equals(comment, swipe1.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(swipe, swiper, swipee, comment);
    }

    @Override
    public String toString() {
        return "Swipe{" +
                "swipe='" + swipe + '\'' +
                ", swiper='" + swiper + '\'' +
                ", swipee='" + swipee + '\'' +
                ", comment='" + comment + '\'' +
                '}';
    }
}
