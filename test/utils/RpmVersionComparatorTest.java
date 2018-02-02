package utils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Brandon Arp (brandon dot arp at inscopemetrics dot com)
 */
@RunWith(Parameterized.class)
public class RpmVersionComparatorTest {
    @Parameterized.Parameters(name = "{index}: compare({0}, {1}) = {2}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {"a01", "a", 1},
                {"1.0", "1.0", 0},
                {"1.0", "2.0", -1},
                {"2.0.1", "2.0.1", 0},
                {"2.0", "2.0.1", -1},
                {"2.0.1a", "2.0.1a", 0},
                {"2.0.1a", "2.0.1", 1},
                {"5.5p1", "5.5p1", 0},
                {"5.5p1", "5.5p2", -1},
                {"5.5p10", "5.5p10", 0},
                {"5.5p1", "5.5p10", -1},
                {"10xyz", "10.1xyz", -1},
                {"xyz10", "xyz10", 0},
                {"xyz10", "xyz10.1", -1},
                {"xyz.4", "xyz.4", 0},
                {"xyz.4", "8", -1},
                {"xyz.4", "2", -1},
                {"5.5p2", "5.6p1", -1},
                {"5.6p1", "6.5p1", -1},
                {"6.0.rc1", "6.0", 1},
                {"10b2", "10a1", 1},
                {"10a2", "10b2", -1},
                {"1.0aa", "1.0aa", 0},
                {"1.0a", "1.0aa", -1},
                {"10.0001", "10.0001", 0},
                {"10.0001", "10.1", 0},
                {"10.1", "10.0001", 0},
                {"10.0001", "10.0039", -1},
                {"4.999.9", "5.0", -1},
                {"20101121", "20101121", 0},
                {"20101121", "20101122", -1},
                {"2_0", "2_0", 0},
                {"2.0", "2_0", 0},
                {"a", "a", 0},
                {"a+", "a+", 0},
                {"a+", "a_", 0},
                {"+a", "+a", 0},
                {"+a", "_a", 0},
                {"+_", "+_", 0},
                {"_+", "+_", 0},
                {"_+", "_+", 0},
                {"+", "_", 0},
                {"_", "+", 0},
                {"1.0~rc1", "1.0~rc1", 0},
                {"1.0~rc1", "1.0", -1},
                {"1.0~rc1", "1.0~rc2", -1},
                {"1.0~rc1~git123", "1.0~rc1~git123", 0},
                {"1.0~rc1~git123", "1.0~rc1", -1},
                {"1.0~rc1", "1.0arc1", -1},
                {"405", "406", -1},
                {"1", "0", 1},
                {"1.0~", "1.0~rc1", -1},
                {"1.0~", "1.0~~rc1", -1},
                {"1.0", "1.0~rc1", 1}
        });
    }

    @Test
    public void compare() {
        Assert.assertEquals(String.format("comparing %s and %s", _o1, _o2), _result, COMPARATOR.compare(_o1, _o2));
        Assert.assertEquals(String.format("comparing %s and %s", _o2, _o1), _result * -1, COMPARATOR.compare(_o2, _o1));
    }

    public RpmVersionComparatorTest(final String o1, final String o2, final int result) {
        _o1 = o1;
        _o2 = o2;
        _result = result;
    }

    private String _o1;
    private String _o2;
    private int _result;
    private static RpmVersionComparator COMPARATOR = new RpmVersionComparator();
}