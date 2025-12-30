require "./spec_helper"

include Protocr::Spec

describe Protocr do
  it "decodes simple" do
    thing = Simple.from_protobuf data("Simple.1")
    thing.has_text?.should be_true
    thing.text.should eq "Hello, world!"
  end

  it "decodes simple empty" do
    thing = Simple.from_protobuf data("Simple.empty")
    thing.has_text?.should be_false
    thing.text.should eq ""
  end

  it "decodes basic" do
    thing = Basic.from_protobuf data("Basic.1")
    thing.has_string?.should be_true
    thing.string.should eq "String"
    thing.has_bytes?.should be_true
    thing.bytes.should eq "0010203040".hexbytes
    thing.has_uint64?.should be_true
    thing.uint64.should eq 42949672950u64
    thing.has_uint32?.should be_true
    thing.uint32.should eq 4294967295u32
    thing.has_other?.should be_true
    # TODO: equality operator
    # thing.other.should eq Other.new(foo: "bar")
    thing.other.has_foo?.should be_true
    thing.other.foo.should eq "bar"
  end

  it "decodes basic partial" do
    thing = Basic.from_protobuf data("Basic.2")
    thing.has_string?.should be_true
    thing.string.should eq "String"
    thing.has_bytes?.should be_false
    thing.bytes.should be_empty
    thing.has_uint64?.should be_true
    thing.uint64.should eq 0u64
    thing.has_uint32?.should be_false
    thing.uint32.should eq 0u32
    thing.has_other?.should be_true
    # TODO: equality operator
    # thing.other.should eq Other.new()
    thing.other.has_foo?.should be_false
    thing.other.foo.should be_empty
  end

  it "decodes incremental original" do
    thing = Incremental.from_protobuf data("Incremental.1")
    thing.has_a?.should be_true
    thing.a.should eq 123u32
    thing.has_b?.should be_true
    thing.b.should eq 456u32
  end

  it "decodes incremental modified" do
    thing = Incremental.from_protobuf data("IncrementalV2.1")
    thing.has_a?.should be_false
    thing.a.should eq 0u32
    thing.has_b?.should be_true
    thing.b.should eq 789u32
    # "key" field has been skipped
  end

  it "decodes incremental old" do
    thing = IncrementalV2.from_protobuf data("Incremental.1")
    thing.has_key?.should be_false
    thing.key.should be_empty
    thing.has_b?.should be_true
    thing.b.should eq 456u32
    # "a" field has been skipped
  end

  it "decodes oneof" do
    thing = Container.from_protobuf data("Container.1")
    thing.has_id?.should be_true
    thing.id.should eq 50u32
    thing.has_x?.should be_true
    # TODO: equality operator
    # thing.x.should eq MessageX.new(value: "meow")
    thing.x.has_value?.should be_true
    thing.x.value.should eq "meow"
    thing.has_y?.should be_false
    # TODO: equality operator
    # thing.y.should eq MessageY.new()
    thing.y.has_value?.should be_false
    thing.y.value.should eq 0u32
  end

  pending "decodes oneof exclusivity" do
    thing = Container.from_protobuf data("ContainerBoth.1")
    thing.has_id?.should be_true
    thing.id.should eq 50u32
    # "y" comes later
    thing.has_x?.should be_false
    # TODO: equality operator
    # thing.x.should eq MessageX.new()
    thing.x.has_value?.should be_false
    thing.x.value.should eq ""
    thing.has_y?.should be_true
    # TODO: equality operator
    # thing.y.should eq MessageY.new(value: 2222u32)
    thing.x.has_value?.should be_true
    thing.x.value.should eq 2222u32
  end

  it "encodes simple" do
    thing = Simple.new
    thing.text = "Hello, world!"
    thing.to_protobuf.should eq data("Simple.1")
  end

  it "encodes simple empty" do
    thing = Simple.new
    thing.to_protobuf.should eq data("Simple.empty")
  end

  it "encodes basic" do
    thing = Basic.new
    thing.string = "String"
    thing.bytes = "0010203040".hexbytes
    thing.uint64 = 42949672950u64
    thing.uint32 = 4294967295u32
    thing.other = Other.new(foo: "bar")
    thing.to_protobuf.should eq data("Basic.1")
  end

  it "encodes basic partial" do
    thing = Basic.new
    thing.string = "String"
    thing.uint64 = 0u64
    thing.other = Other.new()
    thing.to_protobuf.should eq data("Basic.2")
  end

  it "encodes basic to basic partial" do
    thing = Basic.from_protobuf data("Basic.1")
    thing.clear_bytes!
    thing.uint64 = 0u64
    thing.clear_uint32!
    thing.other.clear_foo!
    thing.to_protobuf.should eq data("Basic.2")
  end
end
