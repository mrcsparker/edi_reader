package me.mrcsparker.edireader;

import com.ericsson.otp.erlang.*;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;

@Slf4j
public class JavaServer extends AbstractExecutionThreadService {

    /** Represents the local OTP node. */
    private final OtpNode otpNode;
    /** The name to register for this server's mailbox. */
    private final String registeredProcName;

    /** A mailbox for exchanging messages with Erlang processes. */
    private OtpMbox mbox;

    @Inject
    public JavaServer(
            final OtpNode otpNode,
            @Named("erlang.registered_proc_name") final String registeredProcName) {
        this.otpNode = otpNode;
        this.registeredProcName = registeredProcName;
    }

    /**
     * Run the service. This method is invoked on the execution thread.
     * Implementations must respond to stop requests. You could poll for lifecycle
     * changes in a work loop:
     * <pre>
     *   public void run() {
     *     while ({@link #isRunning()}) {
     *       // perform a unit of work
     *     }
     *   }
     * </pre>
     * ...or you could respond to stop requests by implementing {@link
     * #triggerShutdown()}, which should cause {@link #run()} to return.
     */
    @Override
    protected void run() throws Exception {

        log.info("run()");

        while (isRunning()) {
            final OtpErlangObject msg = mbox.receive();

            log.info("Message received: {}", msg.toString());

            try {
                handle((OtpErlangTuple) msg);
            } catch (final OtpErlangDecodeException | ClassCastException | ArrayIndexOutOfBoundsException e) {
                log.error(e.getMessage());
                log.info("Unrecognised message, ignored.");
            }
        }
    }

    private void handle(final OtpErlangTuple tuple) throws OtpErlangDecodeException {
        final OtpErlangObject[] elements = tuple.elements();
        final OtpErlangAtom opType = (OtpErlangAtom) elements[0];
        switch (opType.atomValue()) {
            case "stop":
                stopAsync();
                break;
            case "$gen_call":
                final OtpErlangTuple from = (OtpErlangTuple) elements[1];
                final OtpErlangTuple req = (OtpErlangTuple) elements[2];
                handleCall(from, req);
                break;
            default:
                final String message = String.format("Bad message: \"%s\"", tuple);
                throw new OtpErlangDecodeException(message);
        }
    }

    private void handleCall(final OtpErlangTuple from, final OtpErlangTuple req)
            throws OtpErlangDecodeException {

        final OtpErlangObject[] elements = req.elements();
        final OtpErlangAtom reqType = (OtpErlangAtom) elements[0];

        log.info("reqType: {}", reqType.atomValue());

        switch (reqType.atomValue()) {
            case "ping":
                reply(from, TypeUtil.tuple(reqType, mbox.self()));
                break;
            case "echo":
                final OtpErlangBinary echo = (OtpErlangBinary) elements[1];
                reply(from, TypeUtil.tuple(reqType, echo));
                break;
            case "edi_file_to_xml":
                final OtpErlangBinary ediFileName = (OtpErlangBinary) elements[1];
                try {
                    EdiFileToXML ediFileToXML = new EdiFileToXML();
                    String result = ediFileToXML.run(new String(ediFileName.binaryValue()));
                    success(from, mbox, convert(result));
                } catch(EdiException e) {
                    failure(from, mbox, e);
                }

                break;
            default:
                reply(from, TypeUtil.tuple(reqType, mbox.self()));
                break;
        }

    }

    public void success(OtpErlangTuple from, OtpMbox mbox, OtpErlangString reply) {
        final OtpErlangObject[] body = new OtpErlangObject[] {
                new OtpErlangAtom("ok"),
                reply
        };

        final OtpErlangObject[] resp = new OtpErlangObject[] {
                from.elementAt(1),  // Ref
                new OtpErlangTuple(body)
        };

        mbox.send((OtpErlangPid) from.elementAt(0), new OtpErlangTuple(resp));
    }

    public void failure(OtpErlangTuple from, OtpMbox mbox, final Exception e) {
        final OtpErlangObject[] body = new OtpErlangObject[] {
                new OtpErlangAtom("error"),
                new OtpErlangString(e.getClass().getSimpleName()),
                new OtpErlangString(e.getLocalizedMessage())
        };

        final OtpErlangObject[] resp = new OtpErlangObject[] {
                from.elementAt(1),  // Ref
                new OtpErlangTuple(body)
        };

        mbox.send((OtpErlangPid) from.elementAt(0), new OtpErlangTuple(resp));
    }


    protected static OtpErlangString convert(String string) {
        return new OtpErlangString(string);
    }

    private void reply(final OtpErlangTuple from, OtpErlangObject reply) {
        final OtpErlangTuple resp = TypeUtil.tuple(from.elementAt(1), reply);
        mbox.send((OtpErlangPid) from.elementAt(0), resp);
    }


    @Override
    protected void startUp() throws Exception {
        mbox = otpNode.createMbox(registeredProcName);
        log.info("Mbox created: {}", registeredProcName);
    }

    @Override
    protected void shutDown() throws Exception {
        mbox.close();
        otpNode.close();
    }
}
