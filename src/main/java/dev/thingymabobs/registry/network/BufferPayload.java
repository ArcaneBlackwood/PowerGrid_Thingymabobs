package dev.thingymabobs.registry.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BufferPayload(CustomPacketPayload.Type<BufferPayload> type, FriendlyByteBuf data) implements CustomPacketPayload {

   public ResourceLocation id() {
      return this.type.id();
   }

   public void release() {
      if (this.data != null && this.data.refCnt() > 0) {
         this.data.release();
      }

   }

   public static StreamCodec<FriendlyByteBuf, BufferPayload> codec(CustomPacketPayload.Type<BufferPayload> type) {
      return StreamCodec.of((buf, payload) -> buf.writeBytes(payload.data, payload.data.readerIndex(), payload.data.readableBytes()), (buf) -> {
         int readableBytes = buf.readableBytes();
         FriendlyByteBuf data = new FriendlyByteBuf(buf.readBytes(readableBytes).asReadOnly());
         return new BufferPayload(type, data);
      });
   }
}
