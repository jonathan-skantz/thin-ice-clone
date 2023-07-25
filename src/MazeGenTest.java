import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Before;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

public class MazeGenTest {

    // NOTE: exactly four possible nodes (not like five nodes but the path is only four)
    private static final int[][] sizesWithFourNodes = new int[][]{{1, 4}, {4, 1}, {2, 2}};
    private static final int[][] sizesWithMaxThreeNodes = new int[][]{{1, 1}, {1, 2}, {2, 1}, {1, 3}, {3, 1}};

    @Before
    public void setup() {
        MazeGen.Amount.priority = new ArrayList<>(MazeGen.Amount.priorityDefault);
    }

    @Test
    public void mazesWithMaxThreeNodesHaveZeroDoublesWhenEndCannotBeDouble() {

        // arrange
        MazeGen.setEndCanBeDouble(false);
        
        for (int[] size : sizesWithMaxThreeNodes) {
            // act
            MazeGen.setSize(size[0], size[1]);
            MazeGen.Amount.GROUND.set(99);
            MazeGen.Amount.DOUBLES.set(99);
            MazeGen.generate();
            
            // assert
            assertThat(MazeGen.Amount.DOUBLES.get(), equalTo(0));
            assertThat(MazeGen.Amount.DOUBLES.nodes.size(), equalTo(0));
            assertThat(MazeGen.Amount.DOUBLES.getMax(), equalTo(0));
        }

    }

    @Test
    public void mazesWithThreeNodesCanOnlyHaveDoublesIfEndCanBeDouble() {
        
        // arrange
        int[][] sizes = new int[][]{{1, 3}, {3, 1}};
        
        for (int[] size : sizes) {
            // act
            MazeGen.Amount.DOUBLES.setPriority(0);

            MazeGen.setEndCanBeDouble(false);
            MazeGen.setSize(size[0], size[1]);
            MazeGen.Amount.GROUND.set(99);
            MazeGen.Amount.DOUBLES.set(99);
            MazeGen.generate();

            // assert
            assertThat(MazeGen.Amount.DOUBLES.get(), equalTo(0));
            assertThat(MazeGen.Amount.DOUBLES.nodes.size(), equalTo(0));
            assertThat(MazeGen.Amount.DOUBLES.getMax(), equalTo(0));
            
            // act #2
            MazeGen.setEndCanBeDouble(true);
            MazeGen.Amount.DOUBLES.set(99);
            MazeGen.Amount.GROUND.set(99);
            MazeGen.generate();

            // assert #2
            assertThat(MazeGen.Amount.DOUBLES.get(), equalTo(2));
            assertThat(MazeGen.Amount.DOUBLES.nodes.size(), equalTo(2));
            assertThat(MazeGen.Amount.DOUBLES.getMax(), equalTo(2));
            // assertThat(MazeGen.endNode.getType(), equalTo(Node.Type.END_DOUBLE));
        }
    }

    @Test
    public void mazesWithFourPossibleNodesCannotHaveOnlyOneDouble() {
        // Even though it is possible with four nodes,
        // the maze must have more than four nodes in total, like:
        // S 2x G
        // -  E -

        // This test prevents configs like:
        // S 2x
        // E  G

        // arrange
        MazeGen.setEndCanBeDouble(false);
        
        for (int[] size : sizesWithFourNodes) {
            // act
            MazeGen.setSize(size[0], size[1]);
            MazeGen.Amount.GROUND.set(99);
            MazeGen.Amount.DOUBLES.set(1);
            MazeGen.generate();
            
            // assert
            assertThat(MazeGen.Amount.DOUBLES.get(), equalTo(0));
            assertThat(MazeGen.Amount.DOUBLES.nodes.size(), equalTo(0));
        }

    }
    
    @Test
    public void mazesWithFourPossibleNodesCanOnlyHaveExactlyOneDoubleIfEndIsDouble() {

        // arrange
        MazeGen.setEndCanBeDouble(true);
        int[][] sizes = new int[][]{{1, 4}, {4, 1}, {2, 2}};
        
        for (int[] size : sizes) {
            // act
            MazeGen.setSize(size[0], size[1]);
            MazeGen.Amount.DOUBLES.setPriority(0);
            MazeGen.Amount.DOUBLES.set(1);
            MazeGen.Amount.GROUND.set(99);      // TODO: why must doubles be set first?
            MazeGen.generate();
            
            // assert
            assertThat(MazeGen.Amount.DOUBLES.get(), equalTo(1));
            assertThat(MazeGen.Amount.DOUBLES.nodes.size(), equalTo(1));
            // assertThat(Node.Type.END_DOUBLE.nodes.size(), equalTo(1));
            // assertThat(MazeGen.endNode.getType(), equalTo(Node.Type.END_DOUBLE));
        }
        
    }
    
    @Test
    public void mazesWithTwoNodesHaveOneGroundAndZeroDoubles() {
        // arrange
        MazeGen.setEndCanBeDouble(false);
        int[][] sizes = new int[][]{{1, 2}, {2, 1}};
        
        for (int[] size : sizes) {
            // act
            MazeGen.setSize(size[0], size[1]);
            MazeGen.Amount.GROUND.set(99);
            MazeGen.Amount.DOUBLES.set(99);
            assertThat(MazeGen.Amount.GROUND.get(), equalTo(1));
            assertThat(MazeGen.Amount.DOUBLES.get(), equalTo(0));
        }
    }

    @Test
    public void mazesWithThreeNodesHaveTwoGroundAndZeroDoublesIfEndCannotBeDouble() {
        // arrange
        MazeGen.setEndCanBeDouble(false);
        int[][] sizes = new int[][]{{1, 3}, {3, 1}};

        for (int[] size : sizes) {
            // act
            MazeGen.setSize(size[0], size[1]);
            MazeGen.Amount.GROUND.set(99);
            MazeGen.Amount.DOUBLES.set(99);
            assertThat(MazeGen.Amount.GROUND.get(), equalTo(2));
            assertThat(MazeGen.Amount.DOUBLES.get(), equalTo(0));
        }
    }

    // @Test
    // public void mazeHasNoNullNodes() {

    //     // TODO: test with endCanBeDouble=true
    //     MazeGen.setEndCanBeDouble(false);

    //     // test all mazes from size 1x1 to 3x3 with all type combinations
    //     for (int height=1; height<=3; height++) {
    //         for (int width=1; width<=3; width++) {

    //             for (int doubles=0; doubles<=6; doubles++) {
    //                 for (int ground=0; ground<=7; ground++) {
                        
    //                     // arrange
    //                     MazeGen.setSize(width, height);
    //                     MazeGen.Amount.DOUBLES.set(doubles);
    //                     MazeGen.Amount.DOUBLES.set(ground);
    //                     MazeGen.generate();
                        
    //                     // act
    //                     for (int y=0; y<MazeGen.getHeight(); y++) {
    //                         for (int x=0; x<MazeGen.getWidth(); x++) {
    //                             // assert
    //                             assertNotNull(MazeGen.get(x, y));
    //                         }
    //                     }

    //                 }
    //             }

    //         }
    //     }

    // }

    // @Test
    // public void mazeWithThreeNodesAndTwoDoublesRequiresEndToBeDouble() {
        
    //     int[][] sizes = new int[][]{{1, 3}, {3, 1}};

    //     for (int[] size : sizes) {
    //         // arrange #1
    //         MazeGen.setSize(size[0], size[1]);
    //         MazeGen.setEndCanBeDouble(false);
    //         MazeGen.Amount.DOUBLES.set(2);
    //         MazeGen.Amount.GROUND.set(0);

    //         // act #1
    //         MazeGen.generate();
            
    //         // assert #1
    //         assertThat(MazeGen.Amount.DOUBLES.getMax(), equalTo(0));
    //         assertThat(MazeGen.Amount.GROUND.get(), equalTo(0));
    //         assertThat(MazeGen.Amount.DOUBLES.nodes.size(), equalTo(0));
            
    //         // arrange #2
    //         MazeGen.setEndCanBeDouble(true);
    //         MazeGen.Amount.DOUBLES.set(2);
    //         MazeGen.Amount.GROUND.set(0);
            
    //         // act #2
    //         MazeGen.generate();

    //         // assert #2
    //         assertThat(MazeGen.Amount.DOUBLES.getMax(), equalTo(2));
    //         assertThat(MazeGen.Amount.GROUND.get(), equalTo(2));
    //         assertThat(MazeGen.Amount.DOUBLES.nodes.size(), equalTo(2));
    //     }


    // }

    @Test
    public void decreasingSizeAdjustsNodeTypesCorrectly() {
        // TODO: for-loop

        MazeGen.setEndCanBeDouble(false);
        MazeGen.Amount.DOUBLES.setPriority(0);
        // TEST WIDTH
        // arrange
        MazeGen.setSize(3, 3);
        MazeGen.Amount.DOUBLES.set(99);
        MazeGen.Amount.GROUND.set(99);

        // act
        MazeGen.setWidth(2);

        // assert
        assertThat(MazeGen.Amount.DOUBLES.get(), equalTo(4));
        assertThat(MazeGen.Amount.GROUND.get(), equalTo(1));
        
        // TEST HEIGHT
        // arrange
        MazeGen.setWidth(3);
        MazeGen.Amount.DOUBLES.set(99);
        MazeGen.Amount.GROUND.set(99);
        
        // act
        MazeGen.setHeight(2);
        
        // assert
        assertThat(MazeGen.Amount.DOUBLES.get(), equalTo(4));
        assertThat(MazeGen.Amount.GROUND.get(), equalTo(1));

    }

}
