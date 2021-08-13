import streamlit as st
import pandas as pd
import json

st.title("Performance evaluation Steps-Parser")

with open('../output.json','r') as f:
    result = json.loads(f.read())

st.write(pd.read_json("../output.json"))
