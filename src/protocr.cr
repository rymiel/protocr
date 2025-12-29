module Protocr
  enum WireType : UInt8
    VarInt  =   0
    I64     =   1
    Len     =   2
    SGroup  =   3
    EGroup  =   4
    I32     =   5
    Invalid = 255
  end

  struct StaticBitset(Bytes)
    @data : UInt8[Bytes]

    def initialize
      @data = StaticArray(UInt8, Bytes).new 0u8
    end

    def test(idx : Int32) : Bool
      byte_idx = idx // 8
      bit_idx = idx % 8
      return ((@data[byte_idx] >> bit_idx) & 1) != 0
    end

    def set(idx : Int32, value : Bool)
      byte_idx = idx // 8
      bit_idx = idx % 8
      if (value)
        @data[byte_idx] |= (1u8 << bit_idx)
      else
        @data[byte_idx] &= ~(1u8 << bit_idx)
      end
    end
  end

  class Reader
    @io : IO

    def initialize(@io)
    end

    def read_varint_u64 : UInt64?
      n = 0u64
      shift = 0
      loop do
        if shift >= 64
          raise "buffer overflow varint"
        end
        byte = @io.read_byte
        return nil if byte.nil?

        n |= ((byte.to_u64 & 0x7F) << shift)
        shift += 7
        if (byte & 0x80) == 0
          return n.to_u64
        end
      end
    end

    def read_varint_u32 : UInt32?
      n = read_varint_u64
      return nil if n.nil?
      n.to_u32
    end

    def read_varint_i32 : Int32?
      n = read_varint_u64
      return nil if n.nil?
      n.to_i32
    end

    def read_string : String?
      n = read_varint_i32
      return nil if n.nil?

      String.new(n) do |buffer|
        @io.read_fully(Slice.new(buffer, n))
        {n, 0}
      end
    end

    def read_bytes : Bytes?
      n = read_varint_i32
      return nil if n.nil?
      bytes = Bytes.new(n)
      @io.read_fully(bytes)
      bytes
    end

    def read_tag : {UInt32, WireType}?
      n = read_varint_u32
      return {0u32, WireType::Invalid} if n.nil?
      field = n >> 3
      wire_type = WireType.new((n & 0x7).to_u8!)

      {field, wire_type}
    end

    def len_subreader(t : T.class) : T? forall T
      slice = read_bytes
      return nil if slice.nil?
      t.new Reader.new IO::Memory.new(slice)
    end

    def skip(wire_type : WireType) : Nil
      case wire_type
      when .len?     then read_bytes # can be optimized
      when .var_int? then read_varint_u64
      else                raise "todo"
      end
    end
  end

  class Writer
    @io : IO

    def initialize(@io)
    end

    def write_varint_u64(n : UInt64) : Nil
      loop do
        b = n & 0x7F
        n >>= 7
        if n == 0
          @io.write_byte(b.to_u8!)
          break
        end
        @io.write_byte (b | 0x80).to_u8!
      end
    end

    def write_varint_u32(n : UInt32) : Nil
      write_varint_u64(n.to_u64!)
    end

    def write_varint_i32(n : Int32) : Nil
      write_varint_u64(n.to_u64!)
    end

    def write_string(str : String) : Nil
      write_bytes(str.encode("UTF-8"))
    end

    def write_bytes(bytes : Bytes) : Nil
      write_varint_u64(bytes.bytesize.to_u64!)
      @io.write(bytes)
    end

    def write_message(msg) : Nil # type constraint?
      bytes = msg.to_protobuf
      write_bytes bytes
    end

    def write_tag(tag : UInt32, wire : WireType)
      write_varint_u64(tag.to_u64! << 3 | wire.to_i)
    end
  end
end
