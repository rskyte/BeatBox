import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.ActionListener;

import static org.mockito.Mockito.*;


public class BeatBoxTest {

    BeatBox subject;
    @Before
    public void beforeEachTest() {
        subject = new BeatBox();
    }

    @Test
    public void firstTest() {
        Assert.assertTrue(true);
    }

    @Test
    public void addingButtonsToButtonBox() {
        Box testBox = new Box(BoxLayout.Y_AXIS);
        subject.addButton("test", mock(ActionListener.class), testBox);
        Assert.assertTrue(testBox.getComponentCount() == 1);
    }
}