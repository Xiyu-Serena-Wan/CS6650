import pandas as pd
import matplotlib.pyplot as plt

threads_df = pd.read_csv("/Users/Xiyu/Downloads/HW1/write 100.csv") # , delimiter=";"
# print(threads_df.head(5))
# print(threads_df.columns);
latency = threads_df['Latency']
# print(latency.describe())
print(latency.mean())
print(latency.median())
print(latency.quantile(0.99))
print(latency.max())
print(latency.min())

threads_df['x'] = (threads_df['Start Timestamp'] - threads_df['Start Timestamp'][0]) // 1000
df2 = threads_df.groupby(['x'])['x'].count()
# print(threads_df['x'])
# print(df2)
df2.plot()
plt.xlabel('time (s)')
plt.ylabel('Throughput per second')

# displaying the title
plt.title("Throughput")
plt.show()
