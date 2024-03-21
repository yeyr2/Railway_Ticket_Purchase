import org.junit.jupiter.api.Test;

import static org.yeyr2.as12306.ticketService.service.handler.ticket.select.SeatSelection.adjacent;
import static org.yeyr2.as12306.ticketService.service.handler.ticket.select.SeatSelection.nonAdjacent;

public class SeatSelectionTest {
    @Test
    void SeatSelection() {
        int[][] seatLayout = {
                {1, 1, 1, 1},
                {1, 1, 1, 0},
                {1, 1, 1, 0},
                {0, 0, 0, 0}
        };
        int[][] select = adjacent(2, seatLayout);
        System.out.println("成功预订相邻座位，座位位置为：");
        assert select != null;
        for (int[] ints : select) {
            System.out.printf("第 %d 排，第 %d 列%n", ints[0], ints[1]);
        }

        int[][] seatLayoutTwo = {
                {1, 0, 1, 1},
                {1, 1, 0, 0},
                {1, 1, 1, 0},
                {0, 0, 0, 0}
        };
        int[][] selectTwo = nonAdjacent(3, seatLayoutTwo);
        System.out.println("成功预订不相邻座位，座位位置为：");
        for (int[] ints : selectTwo) {
            System.out.printf("第 %d 排，第 %d 列%n", ints[0], ints[1]);
        }
    }
}
