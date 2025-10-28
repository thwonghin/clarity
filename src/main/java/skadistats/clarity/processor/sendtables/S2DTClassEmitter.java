package skadistats.clarity.processor.sendtables;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.ZeroCopy;
import skadistats.clarity.event.Event;
import skadistats.clarity.event.Insert;
import skadistats.clarity.event.InsertEvent;
import skadistats.clarity.event.Provides;
import skadistats.clarity.io.Util;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.wire.Packet;
import skadistats.clarity.wire.shared.common.proto.CommonNetMessages;
import skadistats.clarity.wire.shared.demo.proto.Demo;
import skadistats.clarity.wire.shared.s2.proto.S2NetMessages;

import java.io.IOException;

@Provides(value = {OnDTClass.class, OnDTClassesComplete.class}, engine = {EngineId.DOTA_S2, EngineId.CSGO_S2, EngineId.DEADLOCK})
public class S2DTClassEmitter {

    @Insert
    private Context ctx;
    @Insert
    private DTClasses dtClasses;

    @InsertEvent
    private Event<OnDTClassesComplete> evClassesComplete;
    @InsertEvent
    private Event<OnDTClass> evDtClass;

    private FieldGenerator fieldGenerator;

    @OnMessage(Demo.CDemoSendTables.class)
    public void onSendTables(Demo.CDemoSendTables sendTables) throws IOException {
        var cis = CodedInputStream.newInstance(ZeroCopy.extract(sendTables.getData()));
        var protoMessage = Packet.parse(
                S2NetMessages.CSVCMsg_FlattenedSerializer.class,
                ZeroCopy.wrap(cis.readRawBytes(cis.readRawVarint32()))
        );
        onFlattenedSerializers(protoMessage);
    }

    @OnMessage(S2NetMessages.CSVCMsg_FlattenedSerializer.class)
    public void onFlattenedSerializers(S2NetMessages.CSVCMsg_FlattenedSerializer protoMessage) {
        fieldGenerator = new FieldGenerator(
            protoMessage,
            FieldGeneratorPatches.getPatches(ctx.getEngineType().getId(), ctx.getGameVersion()));
        fieldGenerator.createFields();
    }

    @OnMessage(Demo.CDemoClassInfo.class)
    public void onDemoClassInfo(Demo.CDemoClassInfo message) {
        for (var ct : message.getClassesList()) {
            DTClass dt = fieldGenerator.createDTClass(ct.getNetworkName());
            evDtClass.raise(dt);
            dt.setClassId(ct.getClassId());
            dtClasses.byClassId.put(ct.getClassId(), dt);
        }
        dtClasses.classBits = Util.calcBitsNeededFor(dtClasses.byClassId.size() - 1);
        evClassesComplete.raise();
    }

    @OnMessage(CommonNetMessages.CSVCMsg_ClassInfo.class)
    public void onServerClassInfo(CommonNetMessages.CSVCMsg_ClassInfo message) {
        for (var ct : message.getClassesList()) {
            DTClass dt = fieldGenerator.createDTClass(ct.getClassName());
            evDtClass.raise(dt);
            dt.setClassId(ct.getClassId());
            dtClasses.byClassId.put(ct.getClassId(), dt);
        }
        dtClasses.classBits = Util.calcBitsNeededFor(dtClasses.byClassId.size() - 1);
        evClassesComplete.raise();
    }

}
