import streamlit as st
import pandas as pd
import json
import numpy as np
import os
import json
from functools import reduce
st.title("Performance evaluation Module")

dirs = os.listdir( "./experiments" )
experiment = st.selectbox("Select the experiment",dirs)

samples = os.listdir("./experiments/"+experiment)
data = pd.DataFrame()
selector = {}
for x in samples:
    output = json.load(open("./experiments/"+experiment+"/"+x,"r"))
    basic_frame = pd.DataFrame(output["data"])
    if output["gpu"]:
        basic_frame[output["gpu_name"]+":"+output["date"]] = basic_frame["runtime"]/1000000000
        selector[output["gpu_name"]+":"+output["date"]] = (basic_frame,output)
    else:
        basic_frame[output["cpu_name"]+":"+output["date"]] = basic_frame["runtime"]/1000000000
        selector[output["cpu_name"]+":"+output["date"]] = (basic_frame,output)


multi = st.multiselect("Select the samples",list(selector.keys()))

for x in multi:
    st.markdown("**Specification for: "+x+"**")
    st.write("Number of iterations: ",selector[x][1]["iterations_total"])
    st.write("Warumup iterations: ",selector[x][1]["iterations_warmup"])
    st.write("Date of sample: ",selector[x][1]["date"])
    st.write("GPU accelerated: ",selector[x][1]["gpu"])
    with st.expander("Processor details"):
        st.write("Processor: ",selector[x][1]["processor"])
        st.write("Processor name: ",selector[x][1]["cpu_name"])
        st.write("Physical CPU's: ",selector[x][1]["phys_cpus"])
        st.write("Logical CPU's: ",selector[x][1]["log_cpus"])
        st.write("CPU frequency (Hz): ",selector[x][1]["cpu_freq"])

    with st.expander("GPU Details"):
        st.write("Name: ",selector[x][1]["gpu_name"])
    st.write(selector[x][0])


result = reduce(lambda x, y: pd.merge(x, y, how="inner",on = ["document_text","batch_size","lines"]), list(map(lambda x: selector[x][0],multi)))
st.markdown("## Evaluation")
for x in pd.unique(result["document_text"]):
    st.title("=================================================")
    res = result[result["document_text"] == x]
    st.write(res)
    with st.expander("Document text"):
        st.text(x)

    new_obj = {}
    for y in multi:
        new_obj[y] = res[y].to_numpy()
    new_obj["batch_size"] = res["batch_size"].to_numpy()
    new_df = pd.DataFrame(new_obj)
    new_df = new_df.set_index("batch_size")
    st.line_chart(new_df)

st.markdown("### Statistics over whole dataset")
new_agg = {}
for x in multi:
    new_agg[x] = ['max','mean','min']
st.bar_chart(result.agg(new_agg))
for x in multi:
    st.bar_chart(result.agg({x : ['max', 'mean', 'min']}))

st.write(result)
for x in multi:
    st.markdown("Runtime "+x+" by number of lines")
    st.bar_chart(result.groupby(["lines"]).agg(new_agg)[x])

result['length'] = result['document_text'].apply(lambda x: len(x))
for x in multi:
    st.markdown("Runtime "+x+" by number of characters")
    st.bar_chart(result.groupby(["length"]).agg(new_agg)[x])
