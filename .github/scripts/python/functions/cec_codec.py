#!/usr/bin/env python3
import datetime

logical_address_names = {
    0: 'TV',
    1: 'Recording Device 1',
    2: 'Recording Device 2',
    3: 'Tuner 1',
    4: 'Playback Device 1',
    5: 'Audio System',
    6: 'Tuner 2',
    7: 'Tuner 3',
    8: 'Playback Device 2',
    9: 'Reserved',
    10: 'Reserved',
    11: 'TV 2',
    12: 'Playback Device 3',
    13: 'Reserved',
    14: 'Reserved',
    15: 'Free Use',
    16: 'Broadcast'
}


def decode_message(message):
    # Extract the fields from the message tuple
    initiator, destination, timestamp, data = message

    # Convert the initiator and destination fields to logical addresses
    initiator_address = logical_address_names[initiator]
    destination_address = logical_address_names[destination]

    # Convert the timestamp to a datetime object
    timestamp = datetime.datetime.fromtimestamp(timestamp / 1e6)

    # Check if the message is a dict or a string
    if isinstance(data, dict):
        # Extract the opcode and operand data from the dict
        opcode = data['opcode']
        operands = data['parameters']
    else:
        # Parse the data string into a list of integers, skipping the first ten characters
        data_bytes = [int(data[i:i + 2], 16) for i in range(10, len(data), 2)]

        # Extract the opcode from the data bytes
        opcode = data_bytes[0]

        # Extract the operand data from the data bytes
        operands = data_bytes[1:]

    return initiator_address, destination_address, timestamp, opcode, operands


if __name__ == '__main__':
    decoded_message = decode_message((1, 8, 291325, '>> 45:44:41'))
    print(decoded_message)
    decoded_message = decode_message((1, 16, 463288, '<< POLL: Playback 1 (4) -> Audio (5)'))
    print(decoded_message)
