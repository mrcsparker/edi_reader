package me.mrcsparker.edireader;


import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangTuple;

public class TypeUtil {

    static OtpErlangTuple tuple(final OtpErlangObject... objects) {
        return new OtpErlangTuple(objects);
    }
}
