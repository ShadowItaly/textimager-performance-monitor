import streamlit as st
import pandas as pd
import json
import numpy as np
import os
import json
st.title("Performance evaluation Module")

dirs = os.listdir( "./experiments" )
experiment = st.selectbox("Select the experiment",dirs)

samples = os.listdir("./experiments/"+experiment)
data = pd.DataFrame()
selector = {}
for x in samples:
    output = json.load(open("./experiments/"+experiment+"/"+x,"r"))
    if output["gpu"]:
        selector[output["gpu_name"]+":"+output["date"]] = output
    else:
        selector[output["cpu_name"]+":"+output["date"]] = output


multi = st.multiselect("Select the samples",selector.keys())

data = pd.DataFrame(output_gpu["data"])

for x in multi:
    data[x] = pd.DataFrame(selector[x]["data"])
    data[x]["runtime"] = data[x]["runtime"]/1000000000

    with st.beta_expander("Specification for: "+x):
        st.write("Number of iterations: ",selector[x]["iterations_total"])
        st.write("Warumup iterations: ",selector[x]["iterations_warmup"])
        st.write("Date of sample: ",selector[x]["date"])
        with st.beta_expander("Processor details"):
            st.write("Processor: ",selector[x]["processor"])
            st.write("Processor name: ",selector[x]["cpu_name"])
            st.write("Physical CPU's: ",selector[x]["phys_cpus"])
            st.write("Logical CPU's: ",selector[x]["log_cpus"])
            st.write("CPU frequency (Hz): ",selector[x]["cpu_freq"])

        with st.beta_expander("GPU Details"):
            st.write("Name: ",selector[x]["gpu_name"])
        st.write(data)

st.markdown("## Evaluation")
for x in pd.unique(data["document_text"]):
    res = data[data["document_text"] == x]
    st.write(res)
    with st.beta_expander("Document text"):
        st.text(x)

    new_df = pd.DataFrame({"runtime_gpu": res["runtime"].to_numpy(), "runtime_cpu": res["runtime_cpu"].to_numpy(), "batch_size": res["batch_size"].to_numpy()})
    new_df = new_df.set_index("batch_size")
    st.line_chart(new_df[["runtime_gpu","runtime_cpu"]])

st.markdown("### Statistics over whole dataset")
st.bar_chart(data.agg({'runtime' : ['max', 'mean', 'min'], 'runtime_cpu': ['max','mean','min']}))
st.bar_chart(data.agg({'runtime' : ['max', 'mean', 'min']}))
st.bar_chart(data.agg({'runtime_cpu' : ['max', 'mean', 'min']}))

st.markdown("Runtime GPU by number of lines")
st.bar_chart(data.groupby(["lines"]).agg({'runtime': ['max','mean','min'],"runtime_cpu":['max','mean','min']})["runtime"])
st.markdown("Runtime CPU by number of lines")
st.bar_chart(data.groupby(["lines"]).agg({'runtime': ['max','mean','min'],"runtime_cpu":['max','mean','min']})["runtime_cpu"])

data['length'] = data['document_text'].apply(lambda x: len(x))
st.markdown("Runtime GPU by number of characters")
st.bar_chart(data.groupby(["length"]).agg({'runtime': ['max','mean','min'],"runtime_cpu":['max','mean','min']})["runtime"])

st.markdown("Runtime CPU by number of characters")
st.bar_chart(data.groupby(["length"]).agg({'runtime': ['max','mean','min'],"runtime_cpu":['max','mean','min']})["runtime_cpu"])
