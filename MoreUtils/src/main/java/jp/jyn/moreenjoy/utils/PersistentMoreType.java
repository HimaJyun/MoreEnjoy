package jp.jyn.moreenjoy.utils;

import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class PersistentMoreType {
    private PersistentMoreType() {}

    public final static PersistentDataType<int[], UUID> UUID = new PersistentDataType<int[], java.util.UUID>() {
        @Override
        public Class<int[]> getPrimitiveType() {
            return int[].class;
        }

        @Override
        public Class<java.util.UUID> getComplexType() {
            return UUID.class;
        }

        @Override
        public int[] toPrimitive(java.util.UUID complex, PersistentDataAdapterContext context) {
            long most = complex.getMostSignificantBits();
            long least = complex.getLeastSignificantBits();
            return new int[]{
                (int) (most >> 32),
                (int) most,
                (int) (least >> 32),
                (int) least
            };
        }

        @Override
        public java.util.UUID fromPrimitive(int[] primitive, PersistentDataAdapterContext context) {
            long most = (((long) primitive[0]) << 32) | primitive[1];
            long least = (((long) primitive[2]) << 32) | primitive[3];
            return new UUID(most, least);
        }
    };

    public final static PersistentDataType<Byte, Boolean> BOOLEAN = new PersistentDataType<Byte, Boolean>() {
        @Override
        public Class<Byte> getPrimitiveType() {
            return Byte.class;
        }

        @Override
        public Class<Boolean> getComplexType() {
            return Boolean.class;
        }

        @Override
        public Byte toPrimitive(Boolean complex, PersistentDataAdapterContext context) {
            return (byte) (complex ? 1 : 0);
        }

        @Override
        public Boolean fromPrimitive(Byte primitive, PersistentDataAdapterContext context) {
            return primitive == 1 ? Boolean.TRUE : Boolean.FALSE;
        }
    };
}
