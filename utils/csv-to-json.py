import argparse
import csv
import json
import sys

from decimal import Decimal
from re import sub

parser = argparse.ArgumentParser(description='Convert Octavia Street Hilton expenses spreadsheet data to JSON.')
parser.add_argument('--csv', dest='csv_file', metavar='c', type=str, nargs=1,
                   help='an integer for the accumulator')
parser.add_argument('--json', dest='json_file', metavar='j', type=str, nargs='?', default=None,
                   help='output JSON filename')

args = parser.parse_args()

csv_file_name = args.csv_file[0]
try:
  csvfile = open(csv_file_name, 'r')
except Exception as e:
  print 'Could not open CSV file: "%s"' % csv_file_name
  raise e

json_file_name = csv_file_name + '.json'
if args.json_file is not None:
  json_file_name = args.json_file

try:
  jsonfile = open(json_file_name, 'w')
except Exception as e:
  print 'Could not open JSON file to write: "%s"' % json_file_name
  raise e

reader = csv.DictReader(csvfile)
data = []
for row in reader:
    if row['Purchaser'].startswith('...'):
      continue # skip header rows
    date = row['Date'].split('/') # 4/31/2014
    row['Date'] = '%s-%s-%s' % (date[2], date[0].zfill(2), date[1].zfill(2)) # 2014-04-31
    transaction = dict()
    for key, value in row.items():
      key = key.rsplit(' owes...', 1)[0]
      key = key.lower()
      if key in ['amount', 'cartland', 'npstanford', 'rcrabb', 'stromme']:
        if value:
          value = sub(r'[^\d\-.]', '', value)
      transaction[key] = value
    data.append(transaction)

json.dump(data, jsonfile)
