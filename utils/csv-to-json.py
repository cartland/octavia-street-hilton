#!/usr/bin/python
#
# Copyright 2014 Chris Cartland. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


"""
This converts a CSV data from the Octavia Street Hilton expense spreadsheet
into a cleaned up JSON format.
"""
import csv
import json
import sys

from re import sub

def main():
  data = []
  keys = None

  def row_to_dict(row):
    d = dict()
    for i in xrange(len(keys)):
      d[keys[i]] = row[i]
    return d
  for row in csv.reader(iter(sys.stdin.readline, '')):
    if keys is None:
      # The first row in the CSV is the keys
      keys = row
      continue
    row = row_to_dict(row)
    if row['Purchaser'].startswith('...'):
      continue # skip header rows

    # Convert 4/31/2014 to 2014-04-31
    date = row['Date'].split('/')
    row['Date'] = '%s-%s-%s' % (date[2], date[0].zfill(2), date[1].zfill(2))

    transaction = dict()
    for key, value in row.items():
      # Remove ' owes...' from end of any keys
      key = key.rsplit(' owes...', 1)[0]
      # Keys should be lowercase
      key = key.lower()
      if key in ['amount', 'cartland', 'npstanford', 'rcrabb', 'stromme']:
        if value:
          # Sanitize money, keep '-' and '.'
          value = sub(r'[^\d\-.]', '', value)
      if key in ['cartland', 'npstanford', 'rcrabb', 'stromme']:
        if value: # We can skip empty debts
          if not 'debts' in transaction:
            transaction['debts'] = []
          debt = { 'debtor': key, 'amount': value }
          transaction['debts'].append(debt)
      else:
        transaction[key] = value
    data.append(transaction)

  print json.dumps(data)

if __name__ == '__main__':
  main()
