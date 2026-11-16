package com.example.gap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class UpiResponseTest {

    @Test
    public void success_withApprovalRefPreferredAsTransactionId() {
        UpiResponse r = UpiResponse.parse(
                "txnId=AXI123&responseCode=00&Status=SUCCESS&txnRef=REF1&ApprovalRefNo=432112345678");
        assertEquals(UpiResponse.Status.SUCCESS, r.getStatus());
        assertEquals("432112345678", r.getTransactionId());
    }

    @Test
    public void keysAndStatusAreCaseInsensitive() {
        UpiResponse r = UpiResponse.parse("TXNID=abc&STATUS=success");
        assertEquals(UpiResponse.Status.SUCCESS, r.getStatus());
        assertEquals("abc", r.getTransactionId());
    }

    @Test
    public void failureAndPendingAreRecognised() {
        assertEquals(UpiResponse.Status.FAILURE, UpiResponse.parse("Status=FAILURE&txnId=x").getStatus());
        assertEquals(UpiResponse.Status.SUBMITTED, UpiResponse.parse("Status=SUBMITTED").getStatus());
    }

    @Test
    public void missingOrGarbageResponseIsUnknown() {
        assertEquals(UpiResponse.Status.UNKNOWN, UpiResponse.parse(null).getStatus());
        assertEquals(UpiResponse.Status.UNKNOWN, UpiResponse.parse("").getStatus());
        UpiResponse r = UpiResponse.parse("nonsense");
        assertEquals(UpiResponse.Status.UNKNOWN, r.getStatus());
        assertNull(r.getTransactionId());
    }

    @Test
    public void literalNullTransactionIdIsIgnored() {
        assertNull(UpiResponse.parse("Status=SUCCESS&txnId=null").getTransactionId());
    }
}
